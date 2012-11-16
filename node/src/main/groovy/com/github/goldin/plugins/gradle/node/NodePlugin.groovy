package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class NodePlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks () {[ ( NodeConstants.NODE_TEST_TASK ) : NodeTestTask ]}

    @Override
    Map<String , Class> extensions() {[ 'node' : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final tasks = project.tasks.asMap

        if ( tasks.containsKey( 'test' ))
        {
            tasks[ 'test' ].dependsOn( tasks[ NodeConstants.NODE_TEST_TASK ] )
        }
        else
        {
            addTask( project, 'test', NodeTestTask )
        }
    }
}
