package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
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
        printApplicationUrl()
    }


    @Ensures ({ result })
    private String startScript()
    {
        """
        |./target/universal/stage/bin/${ ext.appName } ${ ext.playArguments }${ ext.removeColorCodes } &
        """.stripMargin().toString().trim()
    }


    private void printApplicationUrl()
    {
        final message = "The application is up and running at http://127.0.0.1:${ ext.port }"
        println( "${ '=' * ( message.size() + 2 ) }\n ${ message }\n${ '=' * ( message.size() + 2 ) }" )
    }
}
