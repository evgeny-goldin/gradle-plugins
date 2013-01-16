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
        try
        {
            bashExec( stopScript(), scriptFile( STOP_SCRIPT ))
        }
        finally
        {
            if ( ext.after ) { bashExec( beforeAfterScript( ext.after ), scriptFile( AFTER_STOP_SCRIPT ), false, true, false )}
        }
    }


    @Ensures({ result })
    private String stopScript ()
    {
        """
        |${ baseBashScript() }
        |
        |${ stopCommands().join( '\n|' ) }""".stripMargin()
    }


    @Requires({ ext.stopCommands || ext.scriptPath })
    @Ensures({ result })
    private List<String> stopCommands()
    {
        final List<String> stopCommands =
            ext.stopCommands ?: """
                                |pid=`cat "\$HOME/.forever/pids/${ pidFileName( ext.portNumber ) }"`
                                |if [ "\$pid" != "" ];
                                |then
                                |    foreverId=`forever list | grep \$pid | awk '{print \$2}' | cut -d[ -f2 | cut -d] -f1`
                                |    while [ "\$foreverId" != "" ];
                                |    do
                                |        echo "Stopping forever process [\$foreverId], pid [\$pid]"
                                |        forever stop \$foreverId;
                                |        foreverId=`forever list | grep \$pid | awk '{print \$2}' | cut -d[ -f2 | cut -d] -f1`
                                |    done
                                |fi
                                """.stripMargin().readLines() +
                                ( ext.pidOnlyToStop ? [] :
                                """
                                |
                                |# If .pid file doesn't exist or 'forever stop' fails to stop ..
                                |<kill forever,${ project.name }|${ ext.scriptPath },${ project.name }>
                                """.stripMargin().readLines())

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
