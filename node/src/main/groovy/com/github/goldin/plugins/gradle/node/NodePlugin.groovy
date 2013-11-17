package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import static com.github.goldin.plugins.gradle.common.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.node.tasks.*
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class NodePlugin extends BasePlugin
{
    @SuppressWarnings([' GroovyAssignmentToMethodParameter' ])
    @Requires({ project })
    @Override
    Map<String, Class<? extends NodeBaseTask>> tasks ( Project project )
    {
        final addTasks = extension( project, NODE_EXTENSION, NodeExtension ).addTasks

        ( Map<String , Class<? extends NodeBaseTask>> )(
        [
          ( HELP_TASK          ) : HelpTask,
          ( CLEAN_TASK         ) : CleanTask,
          ( CLEAN_MODULES      ) : CleanModulesTask,
          ( SETUP_TASK         ) : SetupTask,

          ( RUN_TASK           ) : RunTask,
          ( TEST_TASK          ) : TestTask,

          ( START_TASK         ) : StartTask,
          ( LIST_TASK          ) : ListTask,
          ( RESTART_ALL_TASK   ) : RestartAllTask,
          ( CHECK_STARTED_TASK ) : CheckStartedTask,

          ( STOP_TASK          ) : StopTask,
          ( STOP_ALL_TASK      ) : StopAllTask,
          ( CHECK_STOPPED_TASK ) : CheckStoppedTask
        ].
        collectEntries {
            String taskName, Class<? extends NodeBaseTask> taskClass ->
            (( addTasks == null ) || ( addTasks.contains( taskName ))) ? [ taskName, taskClass ] : [:]
        }.
        collectEntries {
            String taskName, Class<? extends NodeBaseTask> taskClass ->
            final otherTask = project.tasks.findByName( taskName )

            if ( otherTask )
            {
                taskName += 'Node'
                otherTask.dependsOn taskName
            }

            [ taskName, taskClass ]
        })
    }


    @Override
    Map<String, Class<NodeExtension>> extensions( Project project ) {[ ( NODE_EXTENSION ) : NodeExtension ]}


    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final setupTask = project.tasks[ SETUP_TASK ]

        [ RUN_TASK, TEST_TASK, START_TASK, LIST_TASK, RESTART_ALL_TASK, STOP_TASK, STOP_ALL_TASK ].each {
            String taskName -> project.tasks.findByName( taskName )?.dependsOn( setupTask )
        }
    }
}
