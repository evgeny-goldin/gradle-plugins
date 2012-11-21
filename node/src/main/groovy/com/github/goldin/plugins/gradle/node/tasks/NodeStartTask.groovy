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
        bashExec( startScript(), scriptPath( START_SCRIPT ), true, ext.generateOnly )
    }


    @Ensures({ result })
    private String startScript()
    {
        """
        ${ bashScript()}

        ${ stopCommands().join( '\n' + ( ' ' * 8 )) }
        $ext.startCommand
        $ext.listCommand""".stripIndent()
    }


    @Requires({ ext.stopCommand })
    @Ensures({ result })
    private List<String> stopCommands()
    {
        final killAllProcesses = find( ext.stopCommand, KillPattern )
        if ( killAllProcesses )
        {
            killAllProcesses.tokenize( '|' )*.trim().grep().collect {
                "ps -Af | grep '$it' | grep -v grep | awk '{print \$2}' | while read pid; do echo \"kill \$pid\"; kill \$pid; done"
            }
        }
        else
        {
            [ ext.stopCommand ]
        }
    }
}
