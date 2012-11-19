package com.github.goldin.plugins.gradle.common

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Base class for all Gradle plugins
 */
abstract class BasePlugin implements Plugin<Project>
{
    @Ensures({ result })
    abstract Map<String, Class<? extends BaseTask>> tasks()

    @Ensures({ result.size() == 1 })
    abstract Map<String, Class> extensions()

    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        final tasks = tasks()

        for ( String taskName in tasks.keySet())
        {
            addTask( project, taskName, tasks[ taskName ] )
        }

        if ( project.logger.infoEnabled )
        {
            project.logger.info(
                "Plugin [${ this.class.name }] is applied, " +
                "added task${ tasks.size() == 1 ? '' : 's' } '${ tasks.keySet().sort().join( '\', \'' )}'." )
        }
    }


    @Requires({ project && taskName && taskClass })
    void addTask( Project project, String taskName, Class<? extends BaseTask> taskClass )
    {
        final  extensions     = extensions()
        final  extensionName  = extensions.keySet().toList().first()
        final  extensionClass = extensions[ extensionName ]
        assert extensionName && extensionClass

        final extension    = project.extensions.findByName( extensionName ) ?:
                             project.extensions.create    ( extensionName, extensionClass )
        final task         = project.tasks.add( taskName, taskClass )
        task.ext           = extension
        task.extensionName = extensionName

        assert extension && task && task.ext && task.extensionName
    }
}
