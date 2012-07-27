package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.tasks.bundling.Jar

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
            final archivePath = archiveServerPlugin( ext, pluginXmlFile )
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
    @Requires({ ext })
    @Ensures({ result.file })
    private File createPluginXml ( AssembleTeamCityPluginExtension ext )
    {
        final pluginXmlContent = this.class.getResourceAsStream( '/teamcity-plugin.xml' ).getText( 'UTF-8' ).
                                 replace( '@name@',         project.name      ).
                                 replace( '@version@',      project.version.toString()).
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
        File             destinationZip    = ext.destinationZip ?:
                                             new File( project.buildDir, "teamcity/${ project.name }-${ project.version }.zip" )
        Collection<File> pluginJars        = (( Collection<Jar>  )( ext.serverJars ?: [ project.tasks[ 'jar' ] ] ))*.archivePath
        Collection<File> configurationJars = ext.serverConfigurations*.files.flatten() as Collection<File>
        Collection<File> teamcityJars      = project.configurations.getByName( 'compile' ).extendsFrom.
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
