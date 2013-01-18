package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Starts Node.js application.
 */
class StartTask extends NodeBaseTask
{
    boolean requiresScriptPath(){ true }

    @Override
    void taskAction()
    {
        if ( ext.stopBeforeStart )
        {   // "after" interceptor is not run when application is stopped before starting it
            final after = ext.after
            ext.after   = []
            runTask ( STOP_TASK )
            ext.after   = after
        }

        if ( ext.before          ) { bashExec( beforeAfterScript( ext.before ), scriptFile( BEFORE_START_SCRIPT ), false, true, false ) }
        bashExec( startScript(), scriptFile( START_SCRIPT ))
        if ( ext.checkAfterStart ) { runTask ( CHECK_STARTED_TASK )}
    }


    @Ensures({ result })
    private String startScript()
    {
        """
        |${ baseBashScript( 'start' ) }
        |export BUILD_ID=JenkinsLetMeSpawn
        |${ startCommands().grep().join( '\n|' ) }""".stripMargin()
    }


    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    private List<String> startCommands()
    {
        final executable = executable()

        [ "forever start ${ ext.foreverOptions ?: '' } --plain --pidFile \"${ pidFileName( ext.portNumber ) }\" " +
          "${ executable ? '"' + executable + '"' : '' } \"${ ext.scriptPath }\" ${ ext.scriptArguments ?: '' }",
          'forever list   --plain' ]
    }
}
