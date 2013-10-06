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
        shellExec( restartallScript(), baseScript())
        if ( ext.checkAfterRestartall ) { runTask( CHECK_STARTED_TASK )}
        if ( ext.listAfterRestartall  ) { runTask( LIST_TASK ) }
    }


    @Ensures({ result })
    private String restartallScript()
    {
        """
        |echo ${ forever() } restartall
        |echo
        |${ forever() } restartall ${ ext.removeColor ? '--plain' : '--colors' }${ ext.removeColorCodes }
        """.stripMargin().toString().trim()
    }
}
