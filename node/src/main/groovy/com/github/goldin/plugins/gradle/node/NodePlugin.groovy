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
          ( project.tasks.findByName( CLEAN_TASK ) ? NODE_CLEAN_TASK : CLEAN_TASK ) : NodeCleanTask,
          ( NODE_SETUP_TASK )                                                       : NodeSetupTask,
          ( project.tasks.findByName( TEST_TASK  ) ? NODE_TEST_TASK  : TEST_TASK  ) : NodeTestTask,
          ( NODE_STOP_TASK  )                                                       : NodeStopTask,
          ( NODE_START_TASK )                                                       : NodeStartTask
        ]
    }

    @Override
    Map<String , Class> extensions( Project project ) {[ ( NODE_EXTENSION ) : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final cleanTask     = project.tasks.getByName( CLEAN_TASK      ) // Should
        final nodeSetupTask = project.tasks.getByName( NODE_SETUP_TASK ) // be
        final testTask      = project.tasks.getByName( TEST_TASK       ) // defined
        final nodeStopTask  = project.tasks.getByName( NODE_STOP_TASK  ) // already
        final nodeStartTask = project.tasks.getByName( NODE_START_TASK ) // by "super.apply( project )" call

        final nodeCleanTask = project.tasks.findByName( NODE_CLEAN_TASK ) // Defined if 'clean' and 'test'
        final nodeTestTask  = project.tasks.findByName( NODE_TEST_TASK  ) // were taken already

        ( nodeTestTask ?: testTask ).dependsOn nodeSetupTask
        nodeStopTask.dependsOn  nodeSetupTask
        nodeStartTask.dependsOn nodeStopTask

        if ( nodeCleanTask ) { cleanTask.dependsOn nodeCleanTask }
        if ( nodeTestTask  ) { testTask. dependsOn nodeTestTask  }
    }
}
