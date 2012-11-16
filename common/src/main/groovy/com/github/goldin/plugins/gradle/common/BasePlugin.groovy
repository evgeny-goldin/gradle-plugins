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
        final extensions = extensions()
        final tasks      = tasks()

        final extensionName  = extensions.keySet().toList().first()
        final extensionClass = extensions[ extensionName ]
        final extension      = project.extensions.create( extensionName, extensionClass )

        assert extensionName && extensionClass && extension

        for ( String taskName in tasks.keySet())
        {
            final task         = project.tasks.add( taskName, tasks[ taskName ] )
            task.extension     = extension
            task.extensionName = extensionName

            assert task && task.extension && task.extensionName
        }

        if ( project.logger.infoEnabled )
        {
            project.logger.info( "Plugin [${ this.class.name }] is applied, " +
                                 "added task${ tasks.size() == 1 ? '' : 's' } '${ tasks.keySet().sort().join( '\', \'' )}', " +
                                 "added extension ${ extensionName }{ .. }" )
        }
    }
}
