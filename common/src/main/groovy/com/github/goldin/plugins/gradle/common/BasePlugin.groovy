package com.github.goldin.plugins.gradle.common

import com.github.goldin.plugins.gradle.common.extensions.BaseExtension
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.lang.reflect.Method


/**
 * Base class for all Gradle plugins
 */
abstract class BasePlugin implements Plugin<Project>
{
    @Requires({ project })
    @Ensures ({ result })
    abstract Map<String, Class<? extends BaseTask>> tasks( Project project )


    @Requires({ project })
    @Ensures ({ result.size() == 1 })
    abstract Map<String, Class<? extends BaseExtension>> extensions( Project project )


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        final  extensions = extensions( project )
        assert extensions.size() == 1

        final  extensionName  = extensions.keySet().toList().first()
        final  extensionClass = extensions[ extensionName ]
        final  extension      = extension( project, extensionName, extensionClass )

        assert extensionName && extensionClass && BaseExtension.isAssignableFrom( extensionClass )

        final tasks = tasks( project )

        for ( String taskName in tasks.keySet())
        {
            addTask( project, taskName, tasks[ taskName ], extensionName, extension )
        }

        project.logger.info(
            "Java version '${ System.getProperty( 'java.version' )}', Groovy version '$GroovySystem.version', " +
            "$project evaluated, plugin '${ this.class.name }' is applied, " +
            "added task${ tasks.size() == 1 ? '' : 's' } '${ tasks.keySet().sort().join( '\', \'' )}'." )
    }


    @Requires({ project && taskName && taskType && extensionName && extension })
    <T extends BaseTask> T addTask( Project project, String taskName, Class<T> taskType, String extensionName, BaseExtension extension )
    {
        final isCreate     = project.tasks.class.methods.any { Method m -> ( m.name == 'create' ) && ( m.parameterTypes == [ String, Class ] )}
        final task         = ( T ) project.tasks."${ isCreate ? 'create' : 'add' }"( taskName, taskType )
        task.extensionName = extensionName
        task.ext           = extension

        assert task && BaseTask.isInstance( task ) && task.ext && task.extensionName && project.tasks[ taskName ]
        task
    }


    @Requires({ project && extensionName && extensionClass })
    <T extends BaseExtension> T extension ( Project project, String extensionName, Class<T> extensionClass )
    {
        final extension = project.extensions.findByName( extensionName ) ?:
                          project.extensions.create    ( extensionName, extensionClass )

        assert BaseExtension.isInstance( extension ) && extensionClass.isInstance( extension )
        ( T ) extension
    }
}
