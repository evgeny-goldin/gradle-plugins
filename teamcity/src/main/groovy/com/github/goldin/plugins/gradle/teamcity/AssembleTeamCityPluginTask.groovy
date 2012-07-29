package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BaseTask
import groovy.text.GStringTemplateEngine
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.bundling.Jar

/**
 * Assembles plugin archive.
 */
class AssembleTeamCityPluginTask extends BaseTask
{
    /**
     * Retrieves task's extension.
     * @return   task's extension
     */
    private AssembleTeamCityPluginExtension ext () { extension ( TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION,
                                                                 AssembleTeamCityPluginExtension ) }

    @Override
    @SuppressWarnings([ 'FileCreateTempFile' ])
    void taskAction()
    {
        final pluginXmlFile  = pluginXmlFile()
        final ext            = ext()
        final isServerPlugin = ( ext.serverProjects || ext.serverConfigurations || ext.serverJarTasks )
        final isAgentPlugin  = ( ext.agentProjects  || ext.agentConfigurations  || ext.agentJarTasks  )

        if ( isServerPlugin )
        {
            final archivePath = archiveServerPlugin( pluginXmlFile )
            logger.info( "Server plugin created at [${ archivePath.canonicalPath }]" )
        }

        assert pluginXmlFile.delete(), "Failed to delete temporary file [${ pluginXmlFile.canonicalPath }]"
    }


    /**
     * Creates temporary file with 'teamcity-plugin.xml' content.
     *
     * @return temporary file with 'teamcity-plugin.xml' content, needs to be deleted later!
     */
    @Ensures({ result.file })
    private File pluginXmlFile ()
    {
        final extension = ext()
        extension.name   ( extension.name    ?: project.name )
        extension.version( extension.version ?: project.version.toString())

        final pluginXmlFile     = File.createTempFile( project.name , null )
        final pluginXmlTemplate =
            new GStringTemplateEngine().createTemplate( this.class.getResource( '/teamcity-plugin.xml' )).
            make([ ext: extension ])

        pluginXmlFile.deleteOnExit()
        pluginXmlFile.withWriter { pluginXmlTemplate.writeTo( it )}
        pluginXmlFile
    }


    /**
     * Archives server-side TeamCity plugin.
     *
     * @param pluginXml file with 'teamcity-plugin.xml' content
     * @return          archive created
     */
    @Requires({ pluginXml.file })
    @Ensures ({ result.file    })
    private File archiveServerPlugin ( File pluginXml )
    {
        AssembleTeamCityPluginExtension ext = ext()

        Collection<Configuration> configurations = ext.serverProjects ? ext.serverProjects*.configurations[ 'compile' ] :
                                                                        ext.serverConfigurations

        assert configurations, "Server configurations are not specified - use either 'serverProjects' or 'serverConfigurations' " +
                               "in ${ TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION }{ .. }"

        Collection<Jar> jarTasks = ext.serverProjects ? ext.serverProjects*.tasks[ 'jar' ] :
                                                        ext.serverJarTasks

        assert jarTasks, "Server jar tasks are not specified - use either 'serverProjects' or 'serverJarTasks' " +
                         "in ${ TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION }{ .. }"

        Collection<File> pluginJars       = jarTasks*.archivePath
        Collection<File> dependenciesJars = configurations*.files.flatten() as Collection<File>
        Collection<File> teamcityJars     = (( Collection<Configuration> ) configurations*.extendsFrom.flatten()).
                                            findAll { it.name.startsWith( 'teamcity' )}*.files.flatten() as Collection<File>

        File destinationZip = ext.destinationZip ?:
                              new File( project.buildDir, "teamcity/${ project.name }-${ project.version }.zip" )
        assert destinationZip.with { ( ! file ) || delete() }

        ant.zip( destfile: destinationZip ) {

            zipfileset( file: pluginXml, fullpath: 'teamcity-plugin.xml' )

            (( Collection<File> )( pluginJars + dependenciesJars - teamcityJars )).each {
                File f ->
                assert f.file , "[${ f.canonicalPath }] - not found when packing [${ destinationZip.canonicalPath }]. " +
                                "Make sure you've correctly specified task \"${ TeamCityPlugin.ASSEMBLE_PLUGIN_TASK }\" dependencies."
                zipfileset( file: f, prefix: 'server' )
            }
        }

        destinationZip
    }
}
