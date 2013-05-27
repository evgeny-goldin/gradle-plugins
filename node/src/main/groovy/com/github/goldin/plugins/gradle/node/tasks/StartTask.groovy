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
                                                          scriptFileForTask( this.name, true ), false, true, true, false, 'before start' ) }

        shellExec( startScript())

        logPidFile()

        if ( ext.checkAfterStart ) { runTask ( CHECK_STARTED_TASK )}
        if ( ext.printUrl        ) { printApplicationUrls() }
        if ( ext.startupScript   ) { addStartupScript() }
    }


    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    private String startScript()
    {
        final coffee  = ext.scriptPath.toLowerCase().endsWith( '.coffee' ) ? COFFEE_EXECUTABLE : ''
        if ( coffee ){ checkFile( coffee )}

        final pidFileName = pidFileName( ext.portNumber )
        final command     = "${ forever() } start -p \"${ foreverHome().canonicalPath }\" --pidFile \"$pidFileName\" " +
                            "--minUptime 5000 --spinSleepTime 5000 ${ ext.foreverOptions ?: '' } " +
                            "${ coffee ? '"' + coffee + '"' : '' } " +
                            "\"${ ext.scriptPath }\" ${ ext.scriptArguments ?: '' }".
                            trim()


        """
        |echo \"Executing $Q${ ext.scriptPath }$Q using port $Q${ ext.portNumber }$Q and PID file $Q${ pidFileName }$Q"
        |echo $command
        |echo
        |$command ${ ext.removeColor ? '--plain' : '--colors' }${ ext.removeColorCodes }
        |${ listProcesses() }
        """.stripMargin().toString().trim()
    }


    private void logPidFile ()
    {
        pidFile().with {
            File f ->
            assert f.file, "PID file [$f.canonicalPath] is not available, application has failed to start or 'forever' isn't working properly"
            final lastModified = f.lastModified()
            final millisAgo    = System.currentTimeMillis() - lastModified

            logger.info( "PID file [$f.canonicalPath] is found, pid is [$f.text], updated $millisAgo millisecond${ s( millisAgo )} ago." )

            assert ( lastModified > startTime ), \
                "[$f.canonicalPath] last modified is $lastModified [${ format( lastModified ) }], " +
                "expected it to be greater than build start time $startTime [$startTimeFormatted]"
        }
    }


    @Requires({ ext.printUrl })
    private void printApplicationUrls ()
    {
        final String localUrl  = "http://127.0.0.1:${ ext.portNumber }${ ext.printUrl }"
        final String publicUrl = ( ext.publicIp ? "http://${ ext.publicIp }:${ ext.portNumber }${ ext.printUrl }" : '' )

        println( "The application is up and running at $localUrl${ publicUrl ? ' (' + publicUrl + ')' : '' }" )
    }


    private void addStartupScript()
    {
        final startupScript = scriptFileForTask( "startup-${ projectName }-${ ext.portNumber }" )
        final currentUser   = exec( 'whoami' )
        File  startupLog    = project.file( 'startup.log' )
        final scripts       = [ scriptFileForTask( SETUP_TASK ).canonicalPath,
                                (( ext.before || ext.beforeStart ) ? scriptFileForTask( this.name, true ).canonicalPath : '' ),
                                scriptFileForTask().canonicalPath ].grep()

        write( startupScript,
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
        |echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ >  "$startupLog.canonicalPath"
        |date                               >> "$startupLog.canonicalPath"
        |echo ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ >> "$startupLog.canonicalPath"
        |su - $currentUser -c '"${ scripts.join( '" && "' )}"' >> "$startupLog.canonicalPath" 2>&1
        """.stripMargin().toString().trim())

        exec( 'chmod', [ '+x', startupScript.canonicalPath ] )
        log { "file:${startupScript.canonicalPath} is created" }
    }
}
