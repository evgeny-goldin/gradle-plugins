package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures


/**
 * Starts Node.js application.
 */
class NodeStartTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        bashExec( startScript(), scriptPath( START_SCRIPT ), true, ext.generateOnly )
    }


    @Ensures({ result })
    private String startScript()
    {
        """
        ${ bashScript()}

        $ext.stopCommand
        $ext.startCommand""".stripIndent()
    }
}
