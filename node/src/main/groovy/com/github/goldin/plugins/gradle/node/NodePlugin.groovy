package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.node.tasks.NodeCleanTask
import com.github.goldin.plugins.gradle.node.tasks.NodeSetupTask
import com.github.goldin.plugins.gradle.node.tasks.NodeStartTask
import com.github.goldin.plugins.gradle.node.tasks.NodeTestTask
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class NodePlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks ( Project p )
    {
        [
          ( p.tasks.findByName( CLEAN_TASK ) ? NODE_CLEAN_TASK : CLEAN_TASK ) : NodeCleanTask,
          ( NODE_SETUP_TASK )                                                 : NodeSetupTask,
          ( p.tasks.findByName( TEST_TASK  ) ? NODE_TEST_TASK  : TEST_TASK  ) : NodeTestTask,
          ( NODE_START_TASK )                                                 : NodeStartTask
        ]
    }

    @Override
    Map<String , Class> extensions( Project p ) {[ ( NODE_EXTENSION ) : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final cleanTask     = project.tasks.getByName( CLEAN_TASK      ) // Should
        final nodeSetupTask = project.tasks.getByName( NODE_SETUP_TASK ) // be
        final testTask      = project.tasks.getByName( TEST_TASK       ) // defined
        final nodeStartTask = project.tasks.getByName( NODE_START_TASK ) //

        final nodeCleanTask = project.tasks.findByName( NODE_CLEAN_TASK ) // Defined if 'clean' and 'test'
        final nodeTestTask  = project.tasks.findByName( NODE_TEST_TASK  ) // were taken already

        ( nodeTestTask ?: testTask ).dependsOn nodeSetupTask
        nodeStartTask.dependsOn nodeSetupTask

        if ( nodeCleanTask ) { cleanTask.dependsOn nodeCleanTask }
        if ( nodeTestTask  ) { testTask. dependsOn nodeTestTask  }
    }
}
