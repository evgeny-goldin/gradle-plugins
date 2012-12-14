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
        if ( ext.stopAndStart )  { runTask ( STOP_TASK )}
        bashExec( startScript(), scriptFile( START_SCRIPT ), true )
        if ( ext.startAndCheck ) { runTask ( CHECK_TASK )}
    }


    @Ensures({ result })
    private String startScript()
    {
        """
        |${ baseBashScript() }
        |export BUILD_ID=JenkinsLetMeSpawn
        |
        |${ startCommands().join( '\n|' )}""".stripMargin()
    }


    @Requires({ ext.startCommands || ext.scriptPath })
    @Ensures({ result })
    private List<String> startCommands()
    {
        if ( ext.startCommands ) { return ext.startCommands }

        String foreverCommand = ''

        if ( ext.isCoffee )
        {
            file( COFFEE_EXECUTABLE ) // Validates existence
            foreverCommand = "\"$COFFEE_EXECUTABLE\""
        }

        [ "forever start --pidFile \"${ pidFileName( ext.portNumber ) }\" $foreverCommand \"$ext.scriptPath\"",
          'forever list' ]
    }
}
