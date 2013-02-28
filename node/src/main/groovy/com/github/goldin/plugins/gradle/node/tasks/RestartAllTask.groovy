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
        bashExec( restartallScript(), taskScriptFile())
        if ( ext.checkAfterRestartall ) { runTask ( CHECK_STARTED_TASK )}
    }


    @Ensures({ result })
    private String restartallScript ()
    {
        """
        |${ baseBashScript() }
        |export BUILD_ID=JenkinsLetMeSpawn
        |${ forever() } restartall
        |${ forever() } list --plain""".stripMargin()
    }
}
