package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures


/**
 * Restarts all currently running Node.js application.
 */
class RestartAllTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        if ( ext.run ) { log{ 'Doing nothing - "run" commands specified' }; return }

        bashExec( restartallScript())
        if ( ext.checkAfterRestartall ) { runTask ( CHECK_STARTED_TASK )}
    }


    @Ensures({ result })
    private String restartallScript ()
    {
        """
        |${ baseBashScript() }
        |
        |echo ${ forever() } restartall
        |${ forever() } restartall${ ext.removeColorCodes }
        |echo $LOG_DELIMITER
        |${ forever() } list${ ext.removeColorCodes }
        |echo $LOG_DELIMITER
        |ps -Af | grep node | grep -v grep
        |echo $LOG_DELIMITER
        """.stripMargin()
    }
}
