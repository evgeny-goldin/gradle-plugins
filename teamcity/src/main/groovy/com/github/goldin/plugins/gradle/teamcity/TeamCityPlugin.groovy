package com.github.goldin.plugins.gradle.teamcity

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class TeamCityPlugin extends BasePlugin
{
    @Override
    String pluginName(){ 'teamcity' }

    @Override
    String extensionName() { 'assembleTeamcityPluginConfig' }

    @Override
    Class extensionClass (){ TeamCityExtension }

    @Override
    String taskName() { 'assembleTeamcityPlugin' }

    @Override
    Class<? extends BaseTask> taskClass() { TeamCityTask }


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final assemblePluginTask = project.tasks.getByName( taskName())
        final tasks              = project.tasks.asMap
        final jarTask            = tasks[ 'jar'   ]
        final testTask           = tasks[ 'test'  ]
        final buildTask          = tasks[ 'build' ]

        if ( jarTask   ) { assemblePluginTask.dependsOn( jarTask.name   )}
        if ( testTask  ) { assemblePluginTask.dependsOn( testTask.name  )}
        if ( buildTask ) { buildTask.dependsOn( assemblePluginTask.name )}
    }
}
