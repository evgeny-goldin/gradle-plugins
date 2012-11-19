package com.github.goldin.plugins.gradle.node

import org.gcontracts.annotations.Ensures

import static com.github.goldin.plugins.gradle.node.NodeConstants.*


/**
 * Starts Node.js application.
 */
class NodeStartTask extends NodeBaseTask
{

    @Override
    void nodeTaskAction()
    {
        bashExec( startScript(), scriptPath( START_SCRIPT ))
    }


    @Ensures({ result })
    private String startScript()
    {
        """
        ${ bashScript()}

        echo "Running '$ext.startCommand'"
        $ext.startCommand""".stripIndent()
    }
}
