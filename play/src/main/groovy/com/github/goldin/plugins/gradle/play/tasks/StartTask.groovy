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

        shellExec( startScript(), baseScript())
    }


    @Ensures ({ result })
    private String startScript()
    {
        """
        |$ext.play stage
        |./target/universal/stage/bin/${ ext.appName } ${ arguments() } &
        """.stripMargin().toString().trim()
    }


    /**
     * Builds application's startup arguments.
     *
     * http://www.playframework.com/documentation/2.2.x/Production
     * http://www.playframework.com/documentation/2.2.x/ProductionConfiguration
     */
    @Ensures ({ result != null })
    private String arguments()
    {
        final arguments = new StringBuilder()

        arguments << " -Dhttp.port='${ ext.port }'"
        arguments << " -Dhttp.address='${ ext.address }'"
        arguments << " -Dconfig.file='${ ext.config }'"
        arguments << " -Dpidfile.path='${RUNNING_PID}'"
        arguments << " ${ ext.arguments }"

        arguments.toString().trim()
    }
}
