package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.node.tasks.*
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class NodePlugin extends BasePlugin
{
    @Requires({ project })
    @Override
    Map<String , Class<? extends BaseTask>> tasks ( Project project )
    {
        [
          ( HELP_TASK          ) : HelpTask,
          ( CLEAN_TASK         ) : CleanTask,
          ( CLEAN_MODULES      ) : CleanModulesTask,
          ( SETUP_TASK         ) : SetupTask,
          ( TEST_TASK          ) : TestTask,

          ( STOP_TASK          ) : StopTask,
          ( STOP_ALL_TASK      ) : StopAllTask,
          ( CHECK_STOPPED_TASK ) : CheckStoppedTask,

          ( START_TASK         ) : StartTask,
          ( RESTART_ALL_TASK   ) : RestartAllTask,
          ( CHECK_STARTED_TASK ) : CheckStartedTask
        ]
    }

    @Override
    Map<String , Class> extensions( Project project ) {[ ( NODE_EXTENSION ) : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final setupTask = project.tasks[ SETUP_TASK ]

        [ TEST_TASK, STOP_TASK, STOP_ALL_TASK, START_TASK, RESTART_ALL_TASK ].each {
            String taskName -> project.tasks[ taskName ].dependsOn( setupTask )
        }
    }
}
