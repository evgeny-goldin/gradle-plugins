package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures


/**
 * Stops all currently running Node.js application.
 */
class StopAllTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        try
        {
            bashExec( stopallScript(), taskScriptFile())
            if ( ext.checkAfterStopall ) { runTask ( CHECK_STOPPED_TASK )}
        }
        finally
        {
            if ( ext.after ) { bashExec( beforeAfterScript( ext.after ), taskScriptFile( false, true ), false, true, false )}
        }
    }


    @Ensures({ result })
    private String stopallScript ()
    {
        """
        |${ baseBashScript( 'stopall' ) }
        |set +e
        |
        |forever stopall
        |forever list --plain
        |
        |${ ext.pidOnlyToStop ? '' : killCommands().join( '\n|' ) }
        |
        |set -e""".stripMargin()
    }
}
