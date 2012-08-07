package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BaseTask
import groovy.xml.MarkupBuilder
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory


/**
 * Assembles TeamCity plugin archive.
 */
class AssembleTeamCityPluginTask extends BaseTask
{
    private static final String BSR = 'buildServerResources'


    private AssembleTeamCityPluginExtension ext() {
        extension( TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION, AssembleTeamCityPluginExtension )
    }


    private buildFile ( String name, String extension = 'zip' ) {
        new File( project.buildDir, "teamcity/$name.$extension" )
    }

    AssembleTeamCityPluginTask (){}


    @TaskAction
    void taskAction()
    {
        /**
         * To avoid multiple tasks executions.
         */
        if ( project.hasProperty( this.class.name )) { return }
        project.ext."${ this.class.name }" = "Executed already"

        final ext        = ext()
        final agentJars  = jars( ext.agentProjects,  ext.agentConfigurations,  ext.agentJarTasks  )
        final serverJars = jars( ext.serverProjects, ext.serverConfigurations, ext.serverJarTasks )

        assert ( agentJars || serverJars ), \
               "Neither of agent or server-related properties specified in ${ TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION }{ .. }"

        final archive = archivePlugin( agentJars, serverJars )
        logger.info( "Plugin archive created at [${ archive.canonicalPath }]" )

        ext.artifactConfigurations.each {
            Configuration configuration ->
            project.artifacts.pushArtifact( configuration, archive, null )
            logger.info( "Plugin archive added as $configuration artifact" )
        }
    }


    /**
     * Retrieves 'agent' or 'server' plugin jars based on projects or configurations & jar tasks specified.
     *
     * @param projects       projects to be used as agent or server plugin
     * @param configurations if projects is null - configurations to be used as plugin dependencies
     * @param jarTasks       if projects is null - tasks creating plugin archive
     * @return               jar files to be packed as 'agent' or 'server' part of the plugin,
     *                       empty collection  if no data is specified.
     */
    Collection<File> jars( Collection<Project>       projects,
                           Collection<Configuration> configurations,
                           Collection<Jar>           jarTasks )
    {
        if ( ! ( projects || ( configurations && jarTasks ))) { return [] }

        Collection<Configuration> dependencies = projects ? projects*.configurations[ 'compile' ] : configurations
        assert dependencies

        Collection<Jar> tasks = projects ? projects*.tasks[ 'jar' ] : jarTasks
        assert tasks

        Collection<File> pluginJars       = tasks*.archivePath
        Collection<File> dependenciesJars = dependencies*.resolve().flatten() as Collection<File>
        Collection<File> teamcityJars     = (( Collection<Configuration> ) dependencies*.extendsFrom.flatten()).
                                            findAll { it.name.startsWith( 'teamcity' )}*.resolve().flatten() as Collection<File>

        ( Collection<File> )( pluginJars + dependenciesJars - teamcityJars )
    }


    /**
     * Creates an archive specified.
     *
     * @param archive     archive to create
     * @param zipClosure  closure to run in {@code ant.zip{ .. }} context
     * @return archive created
     */
    @Requires({ archive && zipClosure })
    @Ensures ({ result.file })
    File archive ( File archive, Closure zipClosure )
    {
        delete( archive )
        ant.zip( destfile: archive, duplicate: 'fail', whenempty: 'fail', level: 9 ){ zipClosure() }
        assert archive.file
        archive
    }


    /**
     * Archives the plugin based on 'agent' and 'server' jars specified.
     *
     * @param agentJars  jar files to be packed as 'agent' plugin's part
     * @param serverJars jar files to be packed as 'agent' plugin's part
     * @return           plugin archive created
     */
    @Requires({ serverJars || agentJars })
    @Ensures ({ result.file })
    File archivePlugin( Collection<File> agentJars, Collection<File> serverJars )
    {
        final ext           = ext()
        final pluginXmlFile = pluginXmlFile()

        assert ext.name && project.name && project.version && pluginXmlFile.file

        File  serverResourcesArchive = null
        final pluginArchive = ( ext.archivePath ?: buildFile( project.name + ( project.version ? "-$project.version" : '' )))
        final agentArchive  = ( agentJars ? ( ext.agentArchivePath ?: buildFile( "${ project.name }-agent-${ project.version }" )) :
                                            null )
        /**
         * ----------------------------------------------------------------------------------
         * Plugins Packaging: http://confluence.jetbrains.net/display/TCD7/Plugins+Packaging
         * ----------------------------------------------------------------------------------
         */

        if ( ext.serverProjects && ( ! ext.serverProjects.any{ new File( it.projectDir, "src/main/resources/$BSR" ).directory } ))
        {
            serverResourcesArchive = archiveServerResources()
            serverJars << serverResourcesArchive
        }

        if ( agentJars )
        {
            archive( agentArchive ) { addFilesToArchive( agentArchive, agentJars, "${ ext.name }/lib", 'agent' )}
        }

        archive( pluginArchive ) {

            ant.zipfileset( file: pluginXmlFile, fullpath: 'teamcity-plugin.xml' )

            if ( agentJars  ){ ant.zipfileset( file: agentArchive, prefix: 'agent' )}
            if ( serverJars ){ addFilesToArchive( pluginArchive, serverJars, 'server', 'plugin' )}

            ext.resources.each {
                Map<String, Object> resources ->
                final FileCollection files    = resources.files as FileCollection
                final String         prefix   = ( resources.prefix != null ) ? resources.prefix : ''
                final List<String>   includes = ( List<String> )( resources.includes ?: [] ).with { delegate instanceof String ? [ delegate ] : delegate }
                final List<String>   excludes = ( List<String> )( resources.excludes ?: [] ).with { delegate instanceof String ? [ delegate ] : delegate }
                assert resources.files, "Specify 'files(..)', 'files : files(..)' or 'files : fileTree(..)' when adding resources to archive"
                addFilesToArchive( pluginArchive, files.files, prefix, 'plugin', includes, excludes )
            }
        }

        delete( serverResourcesArchive, agentArchive, pluginXmlFile )
        assert pluginArchive.file
        pluginArchive
    }


    /**
     * Archives resources file.
     *
     * @return resources file archive
     */
    private File archiveServerResources ()
    {
        final ext                = ext()
        final serverResourcesDir = ext.serverResources

        assert ext.serverProjects
        assert serverResourcesDir, \
            "[$project] - no 'src/main/resources/$BSR' found in 'server' project${ ext.serverProjects.size() == 1 ? '' : 's' } " +
            "${ ext.serverProjects*.projectDir*.canonicalPath }, specify '${ TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION }{ serverResources file( .. ) }'"

        assert serverResourcesDir.directory, \
            "[${ serverResourcesDir.canonicalPath }] - not found"

        assert ( ! serverResourcesDir.listFiles().any { ( it.directory ) && ( it.name == BSR ) }), \
            "'serverResources' [$serverResourcesDir.canonicalPath] should *not* reference a directory containing a '$BSR' directory"

        final resourcesArchive = buildFile( "resources-${ project.version }", 'jar' )
        final files            = project.fileTree( serverResourcesDir ).files
        final basePath         = serverResourcesDir.canonicalPath

        assert files, \
               "No files found in [${ serverResourcesDir.canonicalPath }]"
        assert files.any { it.name.endsWith( '.jsp' )}, \
               "No '*.jsp' files found in [${ serverResourcesDir.canonicalPath }]"

        resourcesArchive.deleteOnExit()

        archive( resourcesArchive ) {
            for ( f in files )
            {
                addFileToArchive( resourcesArchive, f, "$BSR/${ f.canonicalPath - basePath - f.name - '/' }", 'resources' )
            }
        }

        resourcesArchive
    }


    /**
     * Adds files specified to the archive through {@code ant.zipfileset( file: file, prefix: prefix )}.
     *
     * @param archive  archive to add files specified
     * @param files    files to add to the archive
     * @param prefix   files prefix in the archive
     * @param title    archive title to use for error message if any of the files is not found
     * @param includes patterns of files to include, all files are included if null or empty
     * @param excludes patterns of files to exclude, no files are excluded if null or empty
     */
    void addFilesToArchive ( File             archive,
                             Collection<File> files,
                             String           prefix,
                             String           title,
                             List<String>     includes = null,
                             List<String>     excludes = null )
    {
        files.each { addFileToArchive( archive, it, prefix, title, includes, excludes )}
    }


    /**
     * Adds file specified to the archive through {@code ant.zipfileset( file: file, prefix: prefix )}.
     *
     * @param archive  archive to add files specified
     * @param file     file to add to the archive
     * @param prefix   files prefix in the archive
     * @param title    archive title to use for error message if any of the files is not found
     * @param includes patterns of files to include, all files are included if null or empty
     * @param excludes patterns of files to exclude, no files are excluded if null or empty
     */
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    void addFileToArchive ( File         archive,
                            File         file,
                            String       prefix,
                            String       title,
                            List<String> includes = null,
                            List<String> excludes = null )
    {
        assert archive && file && title

        prefix = prefix.startsWith( '/' ) ? prefix.substring( 1 )                      : prefix
        prefix = prefix.endsWith  ( '/' ) ? prefix.substring( 0, prefix.length() - 1 ) : prefix

        assert ( file.file || file.directory ), \
               "[${ file.canonicalPath }] - not found when creating $title archive [${ archive.canonicalPath }]. " +
               "Make sure task \"${ TeamCityPlugin.ASSEMBLE_PLUGIN_TASK }\" dependencies specified correctly"

        final arguments = [ ( file.file ? 'file' : 'dir' ) : file, prefix: prefix ]
        if ( includes ) { arguments[ 'includes' ] = includes.join( ',' )}
        if ( excludes ) { arguments[ 'excludes' ] = excludes.join( ',' )}

        ant.zipfileset( arguments )
    }


    /**
     * Creates temporary file with 'teamcity-plugin.xml' content.
     *
     * @return temporary file with 'teamcity-plugin.xml' content, needs to be deleted later!
     */
    @Ensures({ result.file })
    File pluginXmlFile ()
    {
        final ext = ext()
        ext.name   ( ext.name    ?: project.name )
        ext.version( ext.version ?: project.version.toString())

        final writer  = new StringWriter()
        final builder = new MarkupBuilder( writer )
        final addTag  = {
            String tagName, Object value ->
            if ( value?.toString()?.trim() != '' ){ builder."$tagName"( value.toString().trim()) }
        }

        /**
         * http://confluence.jetbrains.net/display/TCD7/Plugins+Packaging#PluginsPackaging-PluginDescriptor%28%7B%7Bteamcityplugin.xml%7D%7D%29
         */

        builder.doubleQuotes = true
        builder.mkp.xmlDeclaration( version : '1.0', encoding: 'UTF-8' )
        builder.'teamcity-plugin'(
            'xmlns:xsi'                     : 'http://www.w3.org/2001/XMLSchema-instance',
            'xsi:noNamespaceSchemaLocation' : 'urn:shemas-jetbrains-com:teamcity-plugin-v1-xml' ){

            info {
                addTag( 'name',         ext.name        )
                addTag( 'display-name', ext.displayName )
                addTag( 'version',      ext.version     )
                addTag( 'description',  ext.description )
                addTag( 'download-url', ext.downloadUrl )
                addTag( 'email',        ext.email       )
                if ( ext.vendorName || ext.vendorUrl || ext.vendorLogo )
                {
                    vendor {
                        addTag( 'name', ext.vendorName  )
                        addTag( 'url',  ext.vendorUrl   )
                        addTag( 'logo', ext.vendorLogo  )
                    }
                }
            }
            if (( ext.minBuild > -1 ) || ( ext.maxBuild > -1 ))
            {
                requirements([ 'min-build': ext.minBuild, 'max-build': ext.maxBuild ].findAll{ it.value > - 1 })
            }
            deployment( 'use-separate-classloader' : ext.useSeparateClassloader )
            if ( ext.parameters )
            {
                parameters { ext.parameters.each {
                    Map m ->
                    assert m.name,        "Parameter's 'name' should be specified"
                    assert m.value,       "Parameter's 'value' should be specified"
                    assert m.size() == 2, "Only Parameter's 'name' and 'value' should be specified"
                    parameter( name: m.name, m.value )
                }}
            }
        }

        final xml = writer.toString()

        SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI ).
        newSchema( this.class.getResource( '/teamcity-plugin-descriptor.xsd' )).
        newValidator().
        validate( new StreamSource( new StringReader( xml )))

        final tempFile = File.createTempFile( project.name , null )
        tempFile.write( xml, 'UTF-8' )
        tempFile.deleteOnExit()
        tempFile
    }
}
