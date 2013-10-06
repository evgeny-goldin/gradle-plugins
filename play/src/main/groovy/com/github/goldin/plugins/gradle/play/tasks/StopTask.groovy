package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import org.gradle.api.logging.LogLevel


class StopTask extends PlayBaseTask
{
    @Override
    void taskAction()
    {
        final pidFile = project.file( RUNNING_PID )

        if ( pidFile.file )
        {
            runPlay( 'stop' )
            if ( ext.checkAfterStop ) { runTask( CHECK_STOPPED_TASK )}
        }
        else
        {
            log( LogLevel.WARN ){ "'${ pidFile.canonicalPath }' is missing, '$STOP_TASK' task won't be run" }
        }
    }
}
