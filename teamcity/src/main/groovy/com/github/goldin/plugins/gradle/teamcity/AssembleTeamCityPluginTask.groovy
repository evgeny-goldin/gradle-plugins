package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

/**
 * Assembles plugin archive.
 */
class AssembleTeamCityPluginTask extends BaseTask
{
    @Override
    @SuppressWarnings([ 'FileCreateTempFile' ])
    void taskAction()
    {
        final ext           = ( AssembleTeamCityPluginExtension ) project[ TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION ]
        final pluginXmlFile = createPluginXml( ext )

        if ( ext.serverConfigurations )
        {
            final serverPlugin = archiveServerPlugin( ext, pluginXmlFile )
            logger.info( "Server plugin created at [${ serverPlugin.canonicalPath }]" )
        }

        assert pluginXmlFile.delete(), "Failed to delete temp file [${ pluginXmlFile.canonicalPath }]"
    }


    /**
     * Creates temporary file with 'teamcity-plugin.xml' content.
     *
     * @param ext task extension
     * @return temporary file with 'teamcity-plugin.xml' content, needs to be deleted later!
     */
    @Requires({ ext })
    @Ensures({ result.file })
    private File createPluginXml ( AssembleTeamCityPluginExtension ext )
    {
        final pluginXmlContent = this.class.getResourceAsStream( '/teamcity-plugin.xml' ).getText( 'UTF-8' ).
                                 replace( '@name@',         project.name      ).
                                 replace( '@version@',      version.toString()).
                                 replace( '@display-name@', ext.displayName   ).
                                 replace( '@description@',  ext.description   ).
                                 replace( '@vendor-name@',  ext.vendorName    ).
                                 replace( '@vendor-url@',   ext.vendorUrl     )

        final pluginXmlFile    = File.createTempFile( project.name , null )

        pluginXmlFile.deleteOnExit()
        pluginXmlFile.write( pluginXmlContent )
        pluginXmlFile
    }


    /**
     * Archives server-side TeamCity plugin.
     *
     * @param ext       task extension
     * @param pluginXml file with 'teamcity-plugin.xml' content
     * @return          archive created
     */
    @Requires({ ext.serverConfigurations && pluginXml.file })
    @Ensures ({ result.file })
    private File archiveServerPlugin ( AssembleTeamCityPluginExtension ext, File pluginXml )
    {
        assert ( ext.serverJars || jarTask ), \
               "No 'serverJars' specified and 'jar' task is not found in project [$project] - don't know what task archives your code"

        File             destinationZip    = ext.destinationZip ?:
                                             new File( project.buildDir, "teamcity/${ project.name }-${ version }.zip" )
        Collection<File> pluginJars        = ( ext.serverJars ?: [ jarTask ] )*.archivePath
        Collection<File> configurationJars = (( Collection<File> ) ext.serverConfigurations*.files.flatten().toSet()) -
                                             project.configurations.getByName( 'teamcity' ).files

        assert destinationZip.with { ( ! file ) || delete() }

        ant.zip( destfile: destinationZip ) {

            zipfileset( file: pluginXml, fullpath: 'teamcity-plugin.xml' )

            ( pluginJars + configurationJars ).each {
                File f ->
                assert f.file , "[${ f.canonicalPath }] - not found"
                zipfileset( file: f, prefix: 'server' )
            }
        }

        destinationZip
    }
}
