package com.github.goldin.plugins.gradle.teamcity
import com.github.goldin.plugins.gradle.common.BaseTask
import groovy.text.GStringTemplateEngine
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar


/**
 * Assembles TeamCity plugin archive.
 */
class AssembleTeamCityPluginTask extends BaseTask
{
    private AssembleTeamCityPluginExtension ext() { extension( TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION,
                                                               AssembleTeamCityPluginExtension )}

    AssembleTeamCityPluginTask (){}


    @TaskAction
    void taskAction()
    {
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

        (( Collection<File> )( pluginJars + dependenciesJars - teamcityJars )).asImmutable()
    }


    /**
     * Creates a zip archive specified.
     *
     * @param archive     archive to create
     * @param zipClosure  closure to run in {@code ant.zip{ .. }} context
     * @return archive created
     */
    @Requires({ archive && zipClosure })
    @Ensures ({ result.file })
    File zip( File archive, Closure zipClosure )
    {
        delete( archive )

        /**
         * http://evgeny-goldin.org/javadoc/ant/Tasks/zip.html
         */
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

        final archive       = { String name -> new File( project.buildDir, "teamcity/${ name }.zip" )}
        final pluginArchive = ext.archivePath ?: archive( project.name + ( project.version ? "-$project.version" : '' ))
        final agentArchive  = ( agentJars ? ( ext.agentArchivePath ?: archive( "${ project.name }-agent-${ project.version }" )) :
                                            null )

        /**
         * ----------------------------------------------------------------------------------
         * Plugins Packaging: http://confluence.jetbrains.net/display/TCD7/Plugins+Packaging
         * ----------------------------------------------------------------------------------
         */

        if ( agentJars )
        {
            zip( agentArchive ) {
                addFilesToArchive( agentArchive, agentJars, "${ ext.name }/lib", 'agent' )
            }
        }

        zip( pluginArchive ) {

            ant.zipfileset( file: pluginXmlFile, fullpath: 'teamcity-plugin.xml' )

            if ( agentJars )
            {
                ant.zipfileset( file: agentArchive, prefix: 'agent' )
            }

            if ( serverJars )
            {
                addFilesToArchive( pluginArchive, serverJars, 'server', 'plugin' )
            }

            ext.resources.each {
                Map<String, Object> resources ->
                final FileCollection files  = resources.files as FileCollection
                final String         prefix = ( resources.prefix != null ) ? resources.prefix : ''
                assert resources.files, "Specify 'files(..)', 'files : files(..)' or 'files : fileTree(..)' when adding resources to archive"
                addFilesToArchive( pluginArchive, files.files, prefix, 'plugin' )
            }
        }

        delete( agentArchive, pluginXmlFile )
        assert pluginArchive.file
        pluginArchive
    }


    /**
     * Adds files specified to the archive through {@code ant.zipfileset( file: file, prefix: prefix )}.
     *
     * @param archive archive to add files specified
     * @param files   file to add to the archive
     * @param prefix  files prefix in the archive
     * @param title   archive title to use for error message if any of the files is not found
     */
    @Requires({ archive && files && prefix && title })
    void addFilesToArchive ( File archive, Collection<File> files, String prefix, String title )
    {   //noinspection GroovyAssignmentToMethodParameter
        prefix = prefix.startsWith( '/' ) ? prefix.substring( 1 ) : prefix

        files.each {
            File f ->
            assert ( f.file || f.directory ), \
                   "[${ f.canonicalPath }] - not found when creating $title archive [${ archive.canonicalPath }]. " +
                   "Make sure task \"${ TeamCityPlugin.ASSEMBLE_PLUGIN_TASK }\" dependencies specified correctly"

            if ( f.file ) { ant.zipfileset( file: f, prefix: prefix )}
            else          { ant.zipfileset( dir:  f, prefix: prefix )}
        }
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

        final pluginXmlFile     = File.createTempFile( project.name , null )
        final pluginXmlTemplate = new GStringTemplateEngine().createTemplate( this.class.getResource( '/teamcity-plugin.xml' )).
                                  make([ ext: ext ])

        pluginXmlFile.deleteOnExit()
        pluginXmlFile.withWriter { pluginXmlTemplate.writeTo( it )}
        pluginXmlFile
    }
}
