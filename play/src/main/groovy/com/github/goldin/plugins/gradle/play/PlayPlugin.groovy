package com.github.goldin.plugins.gradle.play

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.play.tasks.GruntTask
import com.github.goldin.plugins.gradle.play.tasks.PlayBaseTask
import com.github.goldin.plugins.gradle.play.tasks.RunTask
import com.github.goldin.plugins.gradle.play.tasks.StartTask
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class PlayPlugin extends BasePlugin
{
    @Requires({ project })
    @Override
    Map<String, Class<? extends PlayBaseTask>> tasks ( Project project )
    {
        [
            ( RUN_TASK )   : RunTask,
            ( START_TASK ) : StartTask,
            ( GRUNT_TASK ) : GruntTask,
        ]
    }


    @Requires({ project })
    @Override
    Map<String, Class<PlayExtension>> extensions( Project project ) {[ ( PLAY_EXTENSION ) : PlayExtension ]}
}
