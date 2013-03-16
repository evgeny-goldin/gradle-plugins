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
        if ( ext.run ) { log{ 'Doing nothing - "run" commands specified' }; return }

        try
        {
            bashExec( stopallScript(), taskScriptFile())
            if ( ext.checkAfterStopall ) { runTask ( CHECK_STOPPED_TASK )}
        }
        finally
        {
            if ( ext.after ) { bashExec( commandsScript( ext.after, 'after stopall' ), taskScriptFile( false, true ), false, true, false )}
        }
    }


    @Ensures({ result })
    private String stopallScript ()
    {
        """
        |${ baseBashScript() }
        |set +e
        |
        |echo ${ forever() } stopall
        |${ forever() } stopall
        |${ forever() } list --plain
        |
        |${ ext.pidOnlyToStop ? '' : killCommands().join( '\n|' ) }
        |
        |set -e""".stripMargin()
    }
}
