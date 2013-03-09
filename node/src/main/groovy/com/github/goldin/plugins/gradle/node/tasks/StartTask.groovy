package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import groovy.json.JsonSlurper
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

        if ( ext.before          ) { bashExec( commandsScript( ext.before, 'before start' ), taskScriptFile( true ), false, true, false ) }
        bashExec( startScript(), taskScriptFile())
        if ( ext.checkAfterStart ) { runTask ( CHECK_STARTED_TASK )}
        if ( ext.printUrl        ) { printApplicationUrls() }
    }


    @Ensures({ result })
    private String startScript()
    {
        """
        |${ baseBashScript() }
        |
        |${ startCommands().grep().join( '\n|' ) }""".stripMargin()
    }


    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    private List<String> startCommands()
    {
        final executable  = ext.scriptPath.toLowerCase().endsWith( '.coffee' ) ? COFFEE_EXECUTABLE : ''
        final pidFileName = pidFileName( ext.portNumber )
        final pidFilePath = new File( "${ System.getProperty( 'user.home' )}/.forever/pids/$pidFileName" ).canonicalPath
        final command     = "${ forever() } start ${ ext.foreverOptions ?: '' } --plain --pidFile \"${ pidFileName }\" " +
                            "${ executable ? '"' + executable + '"' : '' } \"${ ext.scriptPath }\" ${ ext.scriptArguments ?: '' }".
                            trim()

        if ( executable )
        {
            final  executableFile = project.file( executable ).canonicalFile
            assert executableFile.file, \
                   "[$executableFile.canonicalPath] is not available, make sure \"coffee-script\" dependency appears in \"package.json\" => \"devDependencies\"\""
        }

        [ "echo \"Executing $Q${ ext.scriptPath }$Q using port $Q${ ext.portNumber }$Q and PID file $Q${ pidFileName }$Q (file:$Q${pidFilePath}$Q)\"",
          "echo \"Running   $Q$command$Q\"",
          command,
          "${ forever() } list --plain" ]
    }


    @Requires({ ext.printUrl })
    void printApplicationUrls ()
    {
        final String externalIp  = (( Map ) new JsonSlurper().parseText( 'http://jsonip.com/'.toURL().text )).ip
        final String internalUrl = "http://127.0.0.1:${ ext.portNumber }${   ext.printUrl == '/' ? '' : ext.printUrl }"
        final String externalUrl = "http://$externalIp:${ ext.portNumber }${ ext.printUrl == '/' ? '' : ext.printUrl }"

        println( "The application is up and running at [$internalUrl] / [$externalUrl]" )
    }
}
