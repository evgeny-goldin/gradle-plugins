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
        if ( ext.run ) { log{ 'Doing nothing - "run" commands specified' }; return }

        if ( ext.stopallBeforeStart || ext.stopBeforeStart )
        {   // "after" interceptor is not run when application is stopped before starting it
            final after = ext.after
            ext.after   = []
            runTask ( ext.stopallBeforeStart ? STOP_ALL_TASK : STOP_TASK )
            ext.after   = after
        }

        if ( ext.before ) { bashExec( commandsScript( ext.before, 'before start' ), taskScriptFile( true ), false, true, false ) }
        bashExec( startScript())

        if ( ext.checkAfterStart        ) { runTask ( CHECK_STARTED_TASK )}
        if ( ext.printUrl               ) { printApplicationUrls() }
        if ( ext.startupScriptDirectory ) { addStartupScript( ext.startupScriptDirectory ) }
    }


    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    private String startScript()
    {
        final executable  = ext.scriptPath.toLowerCase().endsWith( '.coffee' ) ? COFFEE_EXECUTABLE : ''
        final pidFileName = pidFileName( ext.portNumber )
        final pidFilePath = new File( "${ System.getProperty( 'user.home' )}/.forever/pids/$pidFileName" ).canonicalPath
        final command     = "${ forever() } start ${ ext.foreverOptions ?: '' } --pidFile \"${ pidFileName }\" " +
                            "${ executable ? '"' + executable + '"' : '' } \"${ ext.scriptPath }\" ${ ext.scriptArguments ?: '' }".
                            trim()

        if ( executable )
        {
            final  executableFile = project.file( executable ).canonicalFile
            assert executableFile.file, \
                   "[$executableFile.canonicalPath] is not available, make sure \"coffee-script\" dependency appears in \"package.json\" => \"devDependencies\"\""
        }

        """
        |${ baseBashScript() }
        |
        |echo \"Executing $Q${ ext.scriptPath }$Q using port $Q${ ext.portNumber }$Q and PID file $Q${ pidFileName }$Q, file:$Q${ pidFilePath }$Q\"
        |echo $command
        |echo
        |$command${ ext.removeColorCodes }
        |${ listProcesses() }
        """.stripMargin().toString().trim()
    }


    @Requires({ ext.printUrl })
    void printApplicationUrls ()
    {
        final String externalIp  = (( Map ) new JsonSlurper().parseText( 'http://jsonip.com/'.toURL().text )).ip
        final String internalUrl = "http://127.0.0.1:${ ext.portNumber }${   ext.printUrl == '/' ? '' : ext.printUrl }"
        final String externalUrl = "http://$externalIp:${ ext.portNumber }${ ext.printUrl == '/' ? '' : ext.printUrl }"

        println( "The application is up and running at $internalUrl / $externalUrl" )
    }


    @Requires({ directory })
    void addStartupScript( File directory )
    {
        assert ( directory.directory || directory.mkdirs()), "Failed to create [$directory.canonicalPath]"
        final startupScript = new File( directory, "startup-${ projectName }-${ ext.portNumber }.sh" )

        startupScript.write(
        """#!/bin/sh
        |
        |### BEGIN INIT INFO
        |# Provides: ${projectName}
        |# Required-Start: \$remote_fs \$syslog
        |# Required-Stop: \$remote_fs \$syslog
        |# Default-Start: 2 3 4 5
        |# Default-Stop: 0 1 6
        |# Short-Description: ${projectName}
        |# Description: ${projectName}
        |### END INIT INFO
        |
        |${ ext.stopallBeforeStart ? taskScriptFile( false, false, STOP_ALL_TASK ).canonicalPath : ext.stopBeforeStart ? taskScriptFile( false, false, STOP_TASK ).canonicalPath : '' }
        |${ ext.before             ? taskScriptFile( true ).canonicalPath : '' }
        |${ taskScriptFile().canonicalPath }
        """.stripMargin().toString().trim())

        if ( isLinux || isMac ) { exec( 'chmod', [ '+x', startupScript.canonicalPath ]) }

        log { "file:${startupScript.canonicalPath} is created" }
    }
}
