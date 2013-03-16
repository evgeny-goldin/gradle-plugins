package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Stops Node.js application.
 */
class StopTask extends NodeBaseTask
{
    boolean requiresScriptPath(){ ( ! ext.pidOnlyToStop ) }

    @Override
    void taskAction()
    {
        if ( ext.run ) { log{ 'Doing nothing - "run" commands specified' }; return }

        try
        {
            bashExec( stopScript(), taskScriptFile())
            if ( ext.checkAfterStop ) { runTask ( CHECK_STOPPED_TASK )}
        }
        finally
        {
            if ( ext.after ) { bashExec( commandsScript( ext.after, 'after stop' ), taskScriptFile( false, true ), false, true, false )}
        }
    }


    @Ensures({ result })
    private String stopScript ()
    {
        """
        |${ baseBashScript() }
        |${ stopCommands().join( '\n|' ) }""".stripMargin()
    }


    @Requires({ ext.pidOnlyToStop || ext.scriptPath })
    @Ensures({ result })
    private List<String> stopCommands()
    {
        final List<String> stopCommands =
            """
            |pid=`cat "\$HOME/.forever/pids/${ pidFileName( ext.portNumber ) }"`
            |if [ "\$pid" != "" ];
            |then
            |    foreverId=`forever list | grep \$pid | awk '{print \$2}' | cut -d[ -f2 | cut -d] -f1`
            |    while [ "\$foreverId" != "" ];
            |    do
            |        echo "Stopping forever process [\$foreverId], pid [\$pid]"
            |        ${ forever() } stop \$foreverId
            |        foreverId=`forever list | grep \$pid | awk '{print \$2}' | cut -d[ -f2 | cut -d] -f1`
            |    done
            |fi
            """.stripMargin().readLines() +
            ( ext.pidOnlyToStop ? [] :
            """
            |
            |${ killCommands().join( '\n|' )}
            """.stripMargin().readLines())

        ( List<String> ) [ 'set +e', '', *stopCommands, '', 'set -e' ] // Empty commands correspond to empty lines in a bash script
    }
}
