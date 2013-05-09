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
        if ( ext.run ) { log{ 'Doing nothing - "run" commands specified' }; return }

        if ( ext.stopallBeforeStart || ext.stopBeforeStart )
        {   // "after" interceptor is not run when application is stopped before starting it
            final after     = ext.after
            final afterStop = ext.afterStop
            ext.after       = []
            ext.afterStop   = []
            runTask ( ext.stopallBeforeStart ? STOP_ALL_TASK : STOP_TASK )
            ext.after       = after
            ext.afterStop   = afterStop
        }

        if ( ext.before || ext.beforeStart ) { shellExec( commandsScript( add( ext.before, ext.beforeStart )),
                                                          taskScriptFile( true ), false, true, true, false, 'before start' ) }

        shellExec( startScript())

        pidFile().with {
            File f ->
            assert f.file, "[$f.canonicalPath] is not available, application has failed to start or 'forever' isn't working properly"
            final lastModified          = f.lastModified()
            final lastModifiedFormatted = format( lastModified )

            logger.info( "[$f.canonicalPath] - PID is [$f.text], last modified is [$lastModifiedFormatted]" )

            assert ( lastModified > startTime ), \
                "[$f.canonicalPath] last modified is $lastModified [$lastModifiedFormatted], " +
                "expected it to be greater than build start time $startTime [$startTimeFormatted]"
        }

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
        final command     = "${ forever() } start -p \"${ foreverHome().canonicalPath }\" --pidFile \"$pidFileName\" " +
                            "--minUptime 5000 --spinSleepTime 5000 ${ ext.foreverOptions ?: '' } " +
                            "${ executable ? '"' + executable + '"' : '' } " +
                            "\"${ ext.scriptPath }\" ${ ext.scriptArguments ?: '' }".
                            trim()

        if ( executable )
        {
            final  executableFile = project.file( executable ).canonicalFile
            assert executableFile.file, \
                   "[$executableFile.canonicalPath] is not available, make sure \"coffee-script\" dependency appears in \"${ PACKAGE_JSON }\""
        }

        """
        |echo \"Executing $Q${ ext.scriptPath }$Q using port $Q${ ext.portNumber }$Q and PID file $Q${ pidFileName }$Q"
        |echo $command
        |echo
        |$command ${ ext.removeColor ? '--plain' : '--colors' }${ ext.removeColorCodes }
        |${ listProcesses() }
        """.stripMargin().toString().trim()
    }


    @Requires({ ext.printUrl })
    void printApplicationUrls ()
    {
        final String externalIp  = jsonToMap ( httpRequest( 'http://jsonip.com/' ).contentAsString()).ip
        final String internalUrl = "http://127.0.0.1:${ ext.portNumber }${   ext.printUrl == '/' ? '' : ext.printUrl }"
        final String externalUrl = "http://$externalIp:${ ext.portNumber }${ ext.printUrl == '/' ? '' : ext.printUrl }"

        println( "The application is up and running at $internalUrl / $externalUrl" )
    }


    @Requires({ directory })
    void addStartupScript( File directory )
    {
        assert ( directory.directory || directory.mkdirs()), "Failed to create [$directory.canonicalPath]"
        final startupScript = new File( directory, "startup-${ projectName }-${ ext.portNumber }.sh" )
        final currentUser   = exec( 'whoami' )

        startupScript.write(
        """#!${ ext.shell }
        |
        |### BEGIN INIT INFO
        |# Provides:          $projectName
        |# Required-Start:    \$remote_fs \$syslog
        |# Required-Stop:     \$remote_fs \$syslog
        |# Default-Start:     2 3 4 5
        |# Default-Stop:      0 1 6
        |# Short-Description: Start $projectName at boot time
        |# Description:       Start $projectName at boot time
        |### END INIT INFO
        |
        |su - $currentUser -c "${ ext.stopallBeforeStart ? taskScriptFile( false, false, STOP_ALL_TASK ).canonicalPath :
                                  ext.stopBeforeStart    ? taskScriptFile( false, false, STOP_TASK ).canonicalPath :
                                                           '' }"
        |su - $currentUser -c "${ ( ext.before || ext.beforeStart ) ? taskScriptFile( true ).canonicalPath : '' }"
        |su - $currentUser -c "${ taskScriptFile().canonicalPath }"
        """.stripMargin().toString().trim())

        if ( isLinux || isMac ) { exec( 'chmod', [ '+x', startupScript.canonicalPath ]) }

        log { "file:${startupScript.canonicalPath} is created" }
    }
}
