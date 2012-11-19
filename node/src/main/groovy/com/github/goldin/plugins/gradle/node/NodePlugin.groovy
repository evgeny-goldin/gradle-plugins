package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
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
    Map<String , Class<? extends BaseTask>> tasks () {[ ( NODE_SETUP_TASK ) : NodeSetupTask,
                                                        ( NODE_TEST_TASK  ) : NodeTestTask,
                                                        ( NODE_START_TASK ) : NodeStartTask ]}
    @Override
    Map<String , Class> extensions() {[ 'node' : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final nodeSetupTask = project.tasks.getByName( NODE_SETUP_TASK ) // Should
        final nodeTestTask  = project.tasks.getByName( NODE_TEST_TASK  ) // be defined
        final nodeStartTask = project.tasks.getByName( NODE_START_TASK ) // already
        final testTask      = project.tasks.findByName( 'test' )         // May be defined by other plugins

        nodeTestTask.dependsOn  nodeSetupTask
        nodeStartTask.dependsOn nodeSetupTask

        if ( testTask )
        {
            testTask.dependsOn nodeTestTask
        }
        else
        {
            addTask( project, 'test', NodeTestTask )
        }
    }
}
