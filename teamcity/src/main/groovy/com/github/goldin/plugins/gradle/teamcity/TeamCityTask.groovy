package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BaseTask
import groovy.xml.MarkupBuilder
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dsl.DefaultArtifactHandler
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar


/**
 * Assembles TeamCity plugin archive.
 */
class TeamCityTask extends BaseTask<TeamCityExtension>
{
    @Override
    Class extensionType (){ TeamCityExtension }

    private static final String BSR = 'buildServerResources'

    @Override
    void verifyUpdateExtension ( String description )
    {
        ext.name   ( ext.name    ?: project.name )
        ext.version( ext.version ?: project.version.toString())

        assert ext.name,        "'name' hould be defined in $description"
        assert ext.displayName, "'displayName' hould be defined in $description"
        assert ext.version,     "'version' hould be defined in $description"
        assert ext.vendorName,  "'vendorName' hould be defined in $description"
        assert ext.description, "'description' hould be defined in $description"
    }

    /**
     * Archive created
     */
    File archive


    private buildFile( String name, String extension = 'zip' ) { new File( project.buildDir, "teamcity/$name.$extension" )}


    TeamCityTask (){}


    @TaskAction
    void taskAction()
    {
        /**
         * To avoid multiple tasks executions.
         */
        if ( project.hasProperty( this.class.name )) { return }
        project.ext."${ this.class.name }" = "Executed already"

        final agentJars  = jars( ext.agentProjects,  ext.agentConfigurations,  ext.agentJarTasks  )
        final serverJars = jars( ext.serverProjects, ext.serverConfigurations, ext.serverJarTasks )

        assert ( agentJars || serverJars ), \
               "Neither of agent or server-related properties specified in ${ this.extensionName }{ .. }"

        archive = archivePlugin( agentJars, serverJars )
        log{ "Plugin archive created at [${ archive.canonicalPath }]" }

        ext.artifactConfigurations.each {
            Configuration configuration ->
            (( DefaultArtifactHandler ) project.artifacts ).pushArtifact( configuration, archive, null )
            log{ "Plugin archive added as $configuration artifact" }
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

        Collection<Jar>           tasks        = projects ? projects*.tasks[ 'jar' ] : jarTasks
        assert tasks

        Collection<File> pluginJars       = tasks*.archivePath
        Collection<File> dependenciesJars = dependencies*.resolve().flatten() as Collection<File>
        Collection<File> teamcityJars     = (( Collection<Configuration> ) dependencies*.extendsFrom.flatten()).
                                            findAll { it.name.startsWith( 'teamcity' )}*.resolve().flatten() as Collection<File>

        ( Collection<File> )( pluginJars + dependenciesJars - teamcityJars )
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
        final pluginXmlFile = pluginXmlFile()

        assert ext.name && project.name && project.version && pluginXmlFile.file

        final pluginArchive = ( ext.archivePath ?: buildFile( project.name + ( project.version ? "-$project.version" : '' )))
        final agentArchive  = ( agentJars ? ( ext.agentArchivePath ?: buildFile( "${ project.name }-agent-${ project.version }" )) :
                                            null )

        /**
         * ----------------------------------------------------------------------------------
         * Plugins Packaging: http://confluence.jetbrains.net/display/TCD7/Plugins+Packaging
         * ----------------------------------------------------------------------------------
         */

        if ( ext.serverResources )
        {
            serverJars << archiveServerResources()
        }

        if ( agentJars )
        {
            zip( agentArchive ) { addFilesToArchive( agentArchive, agentJars, "${ ext.name }/lib" )}
        }

        zip( pluginArchive ) {

            ant.zipfileset( file: pluginXmlFile, fullpath: 'teamcity-plugin.xml' )

            final checkResourcesAvailable = {
                Collection<File> files, String ... resources ->
                final cl = new URLClassLoader( files*.toURI()*.toURL() as URL[] )
                resources.each { assert cl.getResource( it ), "No '$it' resource found in $files" }
            }

            if ( agentJars  )
            {
                addFileToArchive( pluginArchive, agentArchive, 'agent' )
                checkResourcesAvailable( agentJars, "META-INF/build-agent-plugin-${ ext.name }.xml" )
            }

            if ( serverJars )
            {
                addFilesToArchive( pluginArchive, serverJars, 'server' )
                checkResourcesAvailable( serverJars, BSR, "META-INF/build-server-plugin-${ ext.name }.xml" )
            }

            ext.resources.each {
                Map<String, Object> resources ->
                final FileCollection files    = resources.files as FileCollection
                final String         prefix   = resources.prefix ?: ''
                final List<String>   includes = ( List<String> )( resources.includes ?: [] ).with { delegate instanceof String ? [ delegate ] : delegate }
                final List<String>   excludes = ( List<String> )( resources.excludes ?: [] ).with { delegate instanceof String ? [ delegate ] : delegate }
                assert resources.files, "Specify 'files(..)', 'files : files(..)' or 'files : fileTree(..)' when adding resources to archive"
                addFilesToArchive( pluginArchive, files.files, prefix, includes, excludes )
            }
        }

        delete( agentArchive, pluginXmlFile )
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
        final serverResourcesDir = ext.serverResources
        final path               = serverResourcesDir.canonicalPath

        assert serverResourcesDir?.directory, "[$path] - not found"

        assert ( ! serverResourcesDir.listFiles().any { ( it.directory ) && ( it.name == BSR ) }), \
            "'serverResources' [$path] should *not* reference a directory containing a '$BSR' directory"

        final resourcesArchive = buildFile( "resources-${ project.version }", 'jar' )

        assert project.fileTree( serverResourcesDir ).files.any { it.name.endsWith( '.jsp' )}, \
               "No '*.jsp' files found in [$path]"

        zip( resourcesArchive ){ addFileToArchive( resourcesArchive, serverResourcesDir, BSR ) }
        resourcesArchive.deleteOnExit()
        resourcesArchive
    }


    /**
     * Creates temporary file with 'teamcity-plugin.xml' content.
     *
     * @return temporary file with 'teamcity-plugin.xml' content, needs to be deleted later!
     */
    @Ensures({ result.file })
    File pluginXmlFile ()
    {
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

        final xml      = validateXml( writer.toString(), getResourceText( '/teamcity-plugin-descriptor.xsd' ))
        final tempFile = write( File.createTempFile( project.name, null ), xml )
        tempFile.deleteOnExit()
        tempFile
    }
}
