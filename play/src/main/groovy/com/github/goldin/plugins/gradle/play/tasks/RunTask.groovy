package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.common.CommonConstants.*


class RunTask extends PlayBaseTask
{
    @Override
    void taskAction()
    {
        if ( ext.stopBeforeStart )
        {
            runTask ( STOP_TASK )
        }

        runPlay( '~run', ext.playArguments )
    }
}
