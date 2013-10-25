package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import com.github.goldin.plugins.gradle.common.node.NodeSetupHelper
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.play.PlayExtension
import com.github.goldin.plugins.gradle.play.helpers.PlayHelper
import org.gcontracts.annotations.Requires
import org.gcontracts.annotations.Ensures


/**
 * Base class for all Play plugin tasks.
 */
abstract class PlayBaseTask extends BaseTask<PlayExtension>
{
    @Delegate NodeSetupHelper nodeSetupHelper
    @Delegate PlayHelper      playHelper

    @Override
    Class<PlayExtension> extensionType(){ PlayExtension }


    @Override
    void verifyUpdateExtension ( String description )
    {
        nodeSetupHelper = new NodeSetupHelper( this.project, this, this.ext )
        playHelper      = new PlayHelper     ( this.project, this, this.ext )

        assert ext.appName,          "'appName' should be defined in $description"
        assert ext.playHome,         "'playHome' should be defined in $description"
        assert ext.address,          "'address' should be defined in $description"
        assert ext.config,           "'config' should be defined in $description"
        assert ext.port > 0,         "'port' should be positive in $description"
        assert ext.versions != null, "'versions' should be defined in $description"
        assert ext.defaultVersions,  "'defaultVersions' should be defined in $description"
        assert ext.grunt != null,    "'grunt' should be defined in $description"

        if ( ! ext.updated )
        {
            updateExtension()
            ext.updated = true
        }

        assert ext.nodeVersion,                      "versions['node'] is missing in $description"
        assert ext.playVersion,                      "versions['play'] is missing in $description"
        assert ext.playVersion.startsWith( '2.2.' ), "Only 'playVersion' 2.2.x and higher supported in $description"
    }


    @Requires({ ! ext.updated })
    private void updateExtension()
    {
        ext.versions         = ext.defaultVersions + ext.versions
        ext.nodeVersion      = ext.versions['node']
        ext.playVersion      = ext.versions['play']
        ext.checks           = updateChecks( ext.checks, ext.port )
        ext.playArguments    = playArguments()
        ext.playZip          = "play-${ ext.playVersion }.zip"
        ext.playUrl          = "http://downloads.typesafe.com/play/${ ext.playVersion }/${ ext.playZip }"
        ext.playDirectory    = home( "${ ext.playHome }/play-${ ext.playVersion }" ).canonicalPath
        ext.play             = "'${ ext.playDirectory }/play'"
        ext.removeColorCodes = ( ext.removeColor ? " | $REMOVE_COLOR_CODES" : '' )
        ext.env.PORT         = ext.port

        // https://wiki.jenkins-ci.org/display/JENKINS/Spawning+processes+from+build
        if ( systemEnv.JENKINS_URL != null ){ ext.env.BUILD_ID = 'JenkinsLetMeSpawn' }
    }


    /**
     * Builds application's startup arguments.
     *
     * http://www.playframework.com/documentation/2.2.x/Production
     * http://www.playframework.com/documentation/2.2.x/ProductionConfiguration
     */
    @Ensures ({ result })
    private String playArguments()
    {
        final arguments = new StringBuilder()

        arguments << " -Dhttp.port='$ext.port'"
        arguments << " -Dhttp.address='$ext.address'"
        arguments << " -Dconfig.file='$ext.config'"
        arguments << " -Dpidfile.path='$RUNNING_PID'"
        arguments << " $ext.arguments"

        arguments.toString().trim()
    }


    @Requires({ command && ( arguments != null ) })
    final void runPlay( String command, String arguments = '' )
    {
        shellExec( "$ext.play $arguments $command${ ext.removeColorCodes }".trim(),
                   baseScript( "play $command" ),
                   scriptFileForTask( "play-${ command.replaceAll( /\W/, '' ) }" ))
    }
}
