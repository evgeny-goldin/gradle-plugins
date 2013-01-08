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
        |${ startCommands().grep().join( '\n|' ) }""".stripMargin()
    }


    @Requires({ ext.startCommands || ext.scriptPath })
    @Ensures({ result })
    private List<String> startCommands()
    {
        if ( ext.startCommands ) { return ext.startCommands }

        final executable   = ext.scriptPath.endsWith( '.coffee' ) ? "\"${ file( COFFEE_EXECUTABLE ) }\"" : ''
        final startCommand = ( ext.startWithForever ? "forever start --pidFile \"${ pidFileName( ext.portNumber ) }\"" :
                                                      'node' ) + " $executable \"$ext.scriptPath\""
        final listCommand  = ( ext.startWithForever ? 'forever list' : '' )

        [ startCommand, listCommand ]
    }
}
