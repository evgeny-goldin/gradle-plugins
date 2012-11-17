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
    Map<String , Class<? extends BaseTask>> tasks () {[ 'assembleTeamcityPlugin' : TeamCityTask ]}

    @Override
    Map<String , Class> extensions() {[ 'assembleTeamcityPluginConfig' : TeamCityExtension ]}


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        super.apply( project )

        final assemblePluginTask = project.tasks[ tasks().keySet().toList().first() ]
        final jarTask            = project.tasks.findByName( 'jar' )
        final testTask           = project.tasks.findByName( 'test' )
        final buildTask          = project.tasks.findByName( 'build' )

        if ( jarTask   ) { assemblePluginTask.dependsOn( jarTask.name   )}
        if ( testTask  ) { assemblePluginTask.dependsOn( testTask.name  )}
        if ( buildTask ) { buildTask.dependsOn( assemblePluginTask.name )}
    }
}
