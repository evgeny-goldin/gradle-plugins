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
            bashExec( stopallScript())
            if ( ext.checkAfterStopall ) { runTask ( CHECK_STOPPED_TASK )}
        }
        finally
        {
            if ( ext.after ) { bashExec( commandsScript( ext.after ), taskScriptFile( false, true ), false, true, true, false, 'after stopall' )}
        }
    }


    @Ensures({ result })
    private String stopallScript ()
    {
        """
        |set +e
        |${ listProcesses( false ) }
        |
        |echo ${ forever() } stopall
        |echo
        |${ forever() } stopall${ ext.removeColorCodes }
        |
        |${ ext.pidOnlyToStop ? '' : killProcesses() }
        |${ listProcesses() }
        |
        |set -e
        """.stripMargin().toString().trim()
    }
}
