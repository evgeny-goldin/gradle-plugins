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
    }


    @Ensures ({ result })
    private String startScript()
    {
        """
        |./target/universal/stage/bin/${ ext.appName } ${ arguments() } &
        """.stripMargin().toString().trim()
    }
}
