package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Stops Node.js application.
 */
class NodeStopTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        bashExec( stopScript(), scriptFile( STOP_SCRIPT ), true, ext.generateOnly )
    }


    @Ensures({ result })
    private String stopScript ()
    {
        """
        |${ baseBashScript() }
        |
        |${ stopCommands()*.trim().join( '\n|' ) }""".stripMargin()
    }


    @Requires({ ext.stopCommands || ext.scriptName })
    @Ensures({ result })
    private List<String> stopCommands()
    {
        final List<String> stopCommands =
            ext.stopCommands ?:
            [ "forever stop --pidFile \"${ project.name }.pid\"${ ext.isCoffee ? " \"$NODE_COFFEE_BIN\"" : '' }",
              '',
              "<kill forever,${ project.name }|${ ext.scriptName }>" ]

        final stopCommandsExpanded = stopCommands.collect {

            String stopCommand ->
            assert stopCommand != null, "Undefined stop command [$stopCommand] in $ext.stopCommands"

            final killProcesses = ( stopCommand ? find( stopCommand, KillPattern ) : null /* Empty command*/ )
            if  ( killProcesses )
            {
                killProcesses.trim().tokenize( '|' )*.trim().grep().collect {
                    String process ->

                    final processGrepSteps = process.tokenize( ',' )*.replace( "'", "'\\''" ).collect { "grep '$it'" }.join( ' | ' )
                    final listProcesses    = "ps -Af | $processGrepSteps | grep -v 'grep'"
                    final pids             = "$listProcesses | awk '{print \$2}'"
                    final killAll          = "$pids | while read pid; do echo \"kill \$pid\"; kill \$pid; done"
                    final forceKillAll     = "$pids | while read pid; do echo \"kill -9 \$pid\"; kill -9 \$pid; done"
                    final ifStillRunning   = "if [ \"`$pids`\" != \"\" ]; then"

                    [ "$ifStillRunning $killAll; fi",
                      "$ifStillRunning sleep 5; $forceKillAll; fi",
                      "$ifStillRunning echo 'Failed to kill process [$process]:'; $listProcesses; exit 1; fi" ]
                }.flatten()
            }
            else
            {
                stopCommand
            }
        }.flatten()

        assert stopCommandsExpanded
        [ 'set +e', '', *stopCommandsExpanded, '', 'set -e' ] // Empty commands correspond to empty lines in a bash script
    }
}
