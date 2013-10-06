package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import org.gcontracts.annotations.Ensures


class StartTask extends PlayBaseTask
{
    @Override
    void taskAction()
    {
        if ( ext.stopBeforeStart )
        {
            runTask ( STOP_TASK )
        }

        runPlay( 'stage' )
        shellExec( startScript(), baseScript(), scriptFileForTask(), true, false, false, true )
        if ( ext.checkAfterStart ) { runTask( CHECK_STARTED_TASK )}
    }


    @Ensures ({ result })
    private String startScript()
    {
        """
        |./target/universal/stage/bin/${ ext.appName } ${ arguments() }${ ext.removeColorCodes } &
        """.stripMargin().toString().trim()
    }
}
