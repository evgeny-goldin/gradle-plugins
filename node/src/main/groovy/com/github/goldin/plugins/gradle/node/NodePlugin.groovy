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
          ( CLEAN_TASK      ) : NodeCleanTask,
          ( CLEAN_ALL_TASK  ) : NodeCleanAllTask,
          ( SETUP_TASK      ) : NodeSetupTask,
          ( TEST_TASK       ) : NodeTestTask,
          ( STOP_TASK       ) : NodeStopTask,
          ( START_TASK      ) : NodeStartTask
        ]
    }

    @Override
    Map<String , Class> extensions( Project project ) {[ ( NODE_EXTENSION ) : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final cleanTask    = project.tasks[ CLEAN_TASK     ] // All tasks
        final cleanAllTask = project.tasks[ CLEAN_ALL_TASK ] // should
        final setupTask    = project.tasks[ SETUP_TASK     ] // be
        final testTask     = project.tasks[ TEST_TASK      ] // defined
        final stopTask     = project.tasks[ STOP_TASK      ] // already
        final startTask    = project.tasks[ START_TASK     ] // by "super.apply( project )" call

        cleanAllTask.dependsOn cleanTask
        startTask.   dependsOn  stopTask
    }
}
