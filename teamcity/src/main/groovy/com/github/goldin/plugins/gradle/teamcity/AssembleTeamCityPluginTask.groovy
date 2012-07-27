package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BaseTask
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
        final extension = ( AssembleTeamCityPluginExtension ) project[ TeamCityPlugin.ASSEMBLE_PLUGIN_EXTENSION ]
        final pluginXml = this.class.getResourceAsStream( '/teamcity-plugin.xml' ).getText( 'UTF-8' ).
                          replace( '@name@',         project.name ).
                          replace( '@version@',      version.toString()).
                          replace( '@display-name@', extension.displayName ).
                          replace( '@description@',  extension.description ).
                          replace( '@vendor-name@',  extension.vendorName ).
                          replace( '@vendor-url@',   extension.vendorUrl )

        assert jarTask, "'jar' task is not found in project [$project]"

        final pluginJars = (( Collection<Jar> )( dependsOn.findAll{ it instanceof Jar } + jarTask )).
                           toSet().findAll { it.archivePath.file }*.archivePath
        final tempFile   = File.createTempFile( project.name, null )
        final zip        = extension.destinationZip ?:
                           new File( project.buildDir, "teamcity/${ project.name }-${ version }.zip" )

        tempFile.write( pluginXml )

        if ( 'server' == extension.type )
        {
            ant.zip( destfile: zip ) {
                zipfileset( file: tempFile, fullpath: 'teamcity-plugin.xml' )

                ( pluginJars + project.configurations.compile.files - project.configurations.teamcity.files ).each {
                    File f ->
                    assert f.file, "[$f.canonicalPath] - not found"
                    zipfileset( file: f, prefix : 'server' )
                }
            }
        }
        else
        {
            throw new RuntimeException( 'Not supported yet' )
        }

        assert tempFile.delete()
    }
}
