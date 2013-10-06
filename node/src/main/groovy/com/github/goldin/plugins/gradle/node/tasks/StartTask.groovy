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
                                                          baseScript( 'before start' ),
                                                          scriptFileForTask( this.name, true ), false, true, false ) }

        shellExec( startScript(), baseScript())

        logPidFile()

        if ( ext.checkAfterStart ) { runTask( CHECK_STARTED_TASK )}
        if ( ext.listAfterStart  ) { runTask( LIST_TASK ) }
        if ( ext.printUrl        ) { printApplicationUrls() }
        if ( ext.startupScript   ) { createStartupScript() }
    }


    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    private String startScript()
    {
        final coffee  = ext.scriptPath.toLowerCase().endsWith( '.coffee' ) ? COFFEE_EXECUTABLE : ''
        if ( coffee ){ checkFile( coffee )}

        final pidFileName = pidFileName( ext.port )
        final command     = "${ forever() } start -p \"${ foreverHome().canonicalPath }\" --pidFile \"$pidFileName\" " +
                            "--minUptime 5000 --spinSleepTime 5000 ${ ext.foreverOptions ?: '' } " +
                            "${ coffee ? '"' + project.file( coffee ).canonicalPath + '"' : '' } " +
                            "\"${ project.file( ext.scriptPath ).canonicalPath }\" ${ ext.scriptArguments ?: '' }".
                            trim()


        """
        |echo \"Executing $Q${ ext.scriptPath }$Q using port $Q${ ext.port }$Q and PID file $Q${ pidFileName }$Q"
        |echo $command
        |echo
        |$command ${ ext.removeColor ? '--plain' : '--colors' }${ ext.removeColorCodes }
        """.stripMargin().toString().trim()
    }


    private void logPidFile()
    {
        pidFile().with {
            File f ->

            if ( f.file )
            {
                final lastModified = f.lastModified()
                final millisAgo    = System.currentTimeMillis() - lastModified

                log{ "PID file [$f.canonicalPath] is found, pid is [$f.text], updated $millisAgo millisecond${ s( millisAgo )} ago." }

                assert ( lastModified > startTime ), \
                       "[$f.canonicalPath] last modified is $lastModified [${ format( lastModified ) }], " +
                       "expected it to be greater than build start time $startTime [$startTimeFormatted]"
            }
            else
            {
                failOrWarn( ext.failIfNoPid, "PID file [$f.canonicalPath] is not available, application has failed to start or 'forever' isn't working properly" )
            }
        }
    }


    @Requires({ ext.printUrl })
    private void printApplicationUrls()
    {
        final String localUrl  = "http://127.0.0.1:${ ext.port }${ ext.printUrl }"
        final String publicUrl = ( ext.publicIp ? "http://${ ext.publicIp }:${ ext.port }${ ext.printUrl }" : '' )
        final message          = "The application is up and running at $localUrl${ publicUrl ? ' (' + publicUrl + ')' : '' }"

        println( "${ '=' * ( message.size() + 2 ) }\n ${ message }\n${ '=' * ( message.size() + 2 ) }" )
    }


    private void createStartupScript()
    {
        final startupScript = scriptFileForTask( "startup-${ projectName }-${ ext.port }" )
        final currentUser   = exec( 'whoami' )
        File  startupLog    = project.file( 'startup.log' )
        final scripts       = [ scriptFileForTask( SETUP_TASK ).canonicalPath,
                                (( ext.before || ext.beforeStart ) ? scriptFileForTask( this.name, true ).canonicalPath : '' ),
                                scriptFileForTask().canonicalPath,
                                ext.listAfterStart  ? scriptFileForTask( LIST_TASK ).canonicalPath : '' ].grep()

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
        |""".stripMargin().toString())

        exec( 'chmod', [ '+x', startupScript.canonicalPath ] )
        log { "file:${startupScript.canonicalPath} is created" }
    }
}
