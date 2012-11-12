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
    abstract String taskName()

    @Ensures({ result })
    abstract Class<? extends BaseTask> taskClass()

    @Ensures({ result })
    abstract String extensionName()

    @Ensures({ result })
    abstract Class  extensionClass()


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( taskName(),      taskClass())
        project.extensions.create( extensionName(), extensionClass())
    }
}
