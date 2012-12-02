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
    abstract Map<String, Class<? extends BaseTask>> tasks( Project project )

    @Ensures({ result.size() == 1 })
    abstract Map<String, Class> extensions( Project project )

    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        final tasks = tasks( project )

        for ( String taskName in tasks.keySet())
        {
            addTask( project, taskName, tasks[ taskName ] )
        }

        if ( project.logger.infoEnabled )
        {
            project.logger.info(
                "Groovy version [$GroovySystem.version], plugin [${ this.class.name }] is applied, " +
                "added task${ tasks.size() == 1 ? '' : 's' } '${ tasks.keySet().sort().join( '\', \'' )}'." )
        }
    }


    @Requires({ project && taskName && taskClass })
    <T extends BaseTask> T addTask( Project project, String taskName, Class<T> taskClass )
    {
        final  extensions     = extensions( project )
        final  extensionName  = extensions.keySet().toList().first()
        final  extensionClass = extensions[ extensionName ]
        assert extensionName && extensionClass

        final extension    = project.extensions.findByName( extensionName ) ?:
                             project.extensions.create    ( extensionName, extensionClass )
        final task         = project.tasks.add( taskName, taskClass )
        task.ext           = extension
        task.extensionName = extensionName

        assert extension && task && task.ext && task.extensionName && project.tasks[ taskName ]
        task
    }
}
