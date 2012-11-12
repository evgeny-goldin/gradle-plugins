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
    String pluginName(){ taskName() }

    @Ensures({ result })
    abstract String extensionName()

    @Ensures({ result })
    abstract Class extensionClass()

    @Ensures({ result })
    abstract String taskName()

    @Ensures({ result })
    abstract Class<? extends BaseTask> taskClass()


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        final properties = new Properties()
        properties.load( this.class.classLoader.getResourceAsStream( "META-INF/gradle-plugins/${ pluginName() }.properties" ))
        assert properties[ 'implementation-class' ] == this.class.name

        final extension    = project.extensions.create( extensionName(), extensionClass())
        final task         = project.tasks.add        ( taskName(),      taskClass())
        task.extensionName = extensionName()
        task.extension     = extension

        if ( project.logger.infoEnabled )
        {
            project.logger.info( "Plugin ['${ pluginName() }']/[${ this.class.name }] applied, " +
                                 "added task ['${ taskName() }']/[${ taskClass().name }], " +
                                 "added extension [${ extensionName() }{ .. }]/[${ extensionClass().name }]" )
        }
    }
}
