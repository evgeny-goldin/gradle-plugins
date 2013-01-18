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
          ( CLEAN_TASK         ) : CleanTask,
          ( CLEAN_MODULES      ) : CleanModulesTask,
          ( SETUP_TASK         ) : SetupTask,
          ( TEST_TASK          ) : TestTask,
          ( STOP_TASK          ) : StopTask,
          ( START_TASK         ) : StartTask,
          ( CHECK_STARTED_TASK ) : CheckStartedTask,
          ( CHECK_STOPPED_TASK ) : CheckStoppedTask
        ]
    }

    @Override
    Map<String , Class> extensions( Project project ) {[ ( NODE_EXTENSION ) : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final setupTask = project.tasks[ SETUP_TASK ]
        final testTask  = project.tasks[ TEST_TASK  ]
        final stopTask  = project.tasks[ STOP_TASK  ]
        final startTask = project.tasks[ START_TASK ]

        testTask. dependsOn  setupTask
        stopTask. dependsOn  setupTask
        startTask.dependsOn  setupTask
    }
}
