package com.github.goldin.plugins.gradle.play

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.play.tasks.*
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
            ( SETUP_TASK ) : SetupTask,
            ( RUN_TASK   ) : RunTask,
            ( START_TASK ) : StartTask,
            ( STOP_TASK  ) : StopTask,
            ( GRUNT_TASK ) : GruntTask
        ]
    }


    @Requires({ project })
    @Override
    Map<String, Class<PlayExtension>> extensions( Project project ) {[ ( PLAY_EXTENSION ) : PlayExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final setupTask = project.tasks[ SETUP_TASK ]

        [ RUN_TASK, START_TASK, STOP_TASK, GRUNT_TASK ].each {
            String taskName -> project.tasks[ taskName ].dependsOn( setupTask )
        }
    }

}
