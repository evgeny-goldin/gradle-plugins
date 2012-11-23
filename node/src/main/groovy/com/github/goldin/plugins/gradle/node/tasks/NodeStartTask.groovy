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


    @Ensures({ result })
    private String startScript()
    {
        """
        |${ bashScript() }
        |export BUILD_ID=JenkinsLetMeSpawn
        |
        |${ stopCommands().join( '\n|' ) }
        |$ext.startCommand
        |$ext.listCommand""".stripMargin()
    }


    @Requires({ ext.stopCommand })
    @Ensures({ result })
    private List<String> stopCommands()
    {
        final killProcesses = find( ext.stopCommand, KillPattern )
        if  ( killProcesses )
        {
            [ 'set +e' ] +
            killProcesses.trim().tokenize( '|' )*.trim().grep().collect {
                String process ->
                final processGrep = process.tokenize( ',' )*.replace( "'", "'\\''" ).collect { "grep '$it'" }.join( ' | ' )

                [ "ps -Af | $processGrep | grep -v 'grep' | awk '{print \$2}' | while read pid; do echo \"kill \$pid\"; kill \$pid; done",
                  "sleep 5",
                  "ps -Af | $processGrep | grep -v 'grep' | awk '{print \$2}' | while read pid; do echo \"kill -9 \$pid\"; kill -9 \$pid; done" ]
            }.flatten() +
            [ 'set -e' ]
        }
        else
        {
            [ ext.stopCommand ]
        }
    }
}
