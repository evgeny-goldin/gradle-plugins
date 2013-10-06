package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.ShellHelper
import com.github.goldin.plugins.gradle.play.PlayExtension
import com.github.goldin.plugins.gradle.play.helpers.PlayHelper
import org.gcontracts.annotations.Requires


/**
 * Base class for all Play plugin tasks.
 */
abstract class PlayBaseTask extends BaseTask<PlayExtension>
{
    @Delegate ShellHelper  shellHelper
    @Delegate PlayHelper   playHelper

    @Override
    Class<PlayExtension> extensionType (){ PlayExtension }


    @Override
    void verifyUpdateExtension ( String description )
    {
        shellHelper = new ShellHelper( this.project, this, this.ext )
        playHelper  = new PlayHelper ( this.project, this, this.ext )

        assert ext.appName,       "'appName' should be defined in $description"
        assert ext.playVersion,   "'playVersion' should be defined in $description"
        assert ext.playHome,      "'playHome' should be defined in $description"
        assert ext.address,       "'address' should be defined in $description"
        assert ext.config,        "'config' should be defined in $description"
        assert ext.port      > 0, "'port' should be positive in $description"
        assert ext.debugPort > 0, "'debugPort' should be positive in $description"

        assert ext.playVersion.startsWith( '2.2.' ), "Only 'playVersion' 2.2.x and higher supported in $description"

        if ( ! ext.updated )
        {
            updateExtension()
            ext.updated = true
        }
    }


    private void updateExtension()
    {
        ext.playZip          = "play-${ ext.playVersion }.zip"
        ext.playUrl          = "http://downloads.typesafe.com/play/${ ext.playVersion }/${ ext.playZip }"
        ext.playDirectory    = home( "${ ext.playHome }/play-${ ext.playVersion }" ).canonicalPath
        ext.play             = "'${ ext.playDirectory }/play'"
        ext.removeColorCodes = ( ext.removeColor ? " | $REMOVE_COLOR_CODES" : '' )
    }


    @Requires({ command })
    final void runPlay( String command, String arguments = '' )
    {
        shellExec( "$ext.play $command $arguments${ ext.removeColorCodes }".trim(), baseScript(), scriptFileForTask( "play-$command" ))
    }
}
