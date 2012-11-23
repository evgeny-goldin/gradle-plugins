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
        |${ stopCommands()*.trim().join( '\n|' ) }
        |${ ext.startCommands*.trim().join( '\n|' )}""".stripMargin()
    }


    @Requires({ ext.stopCommands })
    @Ensures({ result })
    private List<String> stopCommands()
    {
        final stopCommands = ext.stopCommands.collect {

            String command ->
            assert command != null, "Undefined stop command [$command] in $ext.stopCommands"

            final killProcesses = ( command ? find( command, KillPattern ) : null /* Empty command*/ )
            if  ( killProcesses )
            {
                killProcesses.trim().tokenize( '|' )*.trim().grep().collect {
                    String process ->
                    final processGrep = process.tokenize( ',' )*.replace( "'", "'\\''" ).collect { "grep '$it'" }.join( ' | ' )

                    [ "ps -Af | $processGrep | grep -v 'grep' | awk '{print \$2}' | while read pid; do echo \"kill \$pid\"; kill \$pid; done",
                      'sleep 5',
                      "ps -Af | $processGrep | grep -v 'grep' | awk '{print \$2}' | while read pid; do echo \"kill -9 \$pid\"; kill -9 \$pid; done",
                      '' ]
                }.flatten()
            }
            else
            {
                command
            }
        }.flatten()

        assert stopCommands
        [ 'set +e', '', *stopCommands, '', 'set -e', '' ] // Empty commands correspond to empty lines in a bash script
    }
}
