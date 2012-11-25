package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Starts Node.js application.
 */
class NodeStartTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        bashExec( startScript(), scriptFile( START_SCRIPT ), true, ext.generateOnly )
    }


    @Requires({ ext.startCommands || ext.scriptPath })
    @Ensures({ result })
    private String startScript()
    {
        final List<String> startCommands =
            ext.startCommands ?:
            [ "forever start --pidFile \"${ project.name }.pid\"${ ext.isCoffee ? " \"$NODE_COFFEE_BIN\"" : '' } \"$ext.scriptPath\"" ]

        """
        |${ baseBashScript() }
        |export BUILD_ID=JenkinsLetMeSpawn
        |
        |${ startCommands*.trim().join( '\n|' )}""".stripMargin()
    }
}
