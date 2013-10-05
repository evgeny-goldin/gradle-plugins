package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import org.gradle.api.logging.LogLevel
import org.gcontracts.annotations.Ensures


class StopTask extends PlayBaseTask
{
    @Override
    void taskAction()
    {
        final pidFile = project.file( RUNNING_PID )

        if ( pidFile.file )
        {
            shellExec( stopScript(), baseScript())
        }
        else
        {
            log( LogLevel.WARN ){ "'${ pidFile.canonicalPath }' is missing, '$STOP_TASK' task won't be run" }
        }
    }


    @Ensures ({ result })
    private String stopScript()
    {
        """
        |$ext.play stop
        """.stripMargin().toString().trim()
    }
}
