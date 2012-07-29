package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.tasks.bundling.Jar
import groovy.text.GStringTemplateEngine

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
        final ext = ext()
        ext.name   ( ext.name    ?: project.name )
        ext.version( ext.version ?: project.version.toString())

        final pluginXmlFile = pluginXmlFile()

        if ( ext.serverConfigurations )
        {
            final archivePath = archiveServerPlugin( pluginXmlFile )
            logger.info( "Server plugin created at [${ archivePath.canonicalPath }]" )
        }

        assert pluginXmlFile.delete(), "Failed to delete temporary file [${ pluginXmlFile.canonicalPath }]"
    }


    /**
     * Creates temporary file with 'teamcity-plugin.xml' content.
     *
     * @param ext task extension
     * @return temporary file with 'teamcity-plugin.xml' content, needs to be deleted later!
     */
    @Ensures({ result.file })
    private File pluginXmlFile ()
    {
        final pluginXmlFile     = File.createTempFile( project.name , null )
        final pluginXmlTemplate =
            new GStringTemplateEngine().createTemplate( this.class.getResource( '/teamcity-plugin.xml' )).
            make([ ext: ext() ])

        pluginXmlFile.deleteOnExit()
        pluginXmlFile.withWriter { pluginXmlTemplate.writeTo( it )}
        pluginXmlFile
    }


    /**
     * Archives server-side TeamCity plugin.
     *
     * @param ext       task extension
     * @param pluginXml file with 'teamcity-plugin.xml' content
     * @return          archive created
     */
    @Requires({ ext().serverConfigurations && pluginXml.file })
    @Ensures ({ result.file })
    private File archiveServerPlugin ( File pluginXml )
    {
        AssembleTeamCityPluginExtension ext = ext()
        File             destinationZip     = ext.destinationZip ?:
                                              new File( project.buildDir, "teamcity/${ project.name }-${ project.version }.zip" )
        Collection<File> pluginJars         = (( Collection<Jar>  )( ext.serverJars ?: [ project.tasks[ 'jar' ] ] ))*.archivePath
        Collection<File> configurationJars  = ext.serverConfigurations*.files.flatten() as Collection<File>
        Collection<File> teamcityJars       = project.configurations.getByName( 'compile' ).extendsFrom.
                                              findAll { name.startsWith( 'teamcity ')}*.files.flatten() as Collection<File>

        assert destinationZip.with { ( ! file ) || delete() }

        ant.zip( destfile: destinationZip ) {

            zipfileset( file: pluginXml, fullpath: 'teamcity-plugin.xml' )

            (( Collection<File> )( pluginJars + configurationJars - teamcityJars )).toSet().each {
                File f ->
                assert f.file , "[${ f.canonicalPath }] - not found"
                zipfileset( file: f, prefix: 'server' )
            }
        }

        destinationZip
    }
}
