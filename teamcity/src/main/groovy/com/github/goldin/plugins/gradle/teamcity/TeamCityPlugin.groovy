package com.github.goldin.plugins.gradle.teamcity

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class TeamCityPlugin implements Plugin<Project>
{
    static final String TASK_NAME      = 'assembleTeamcityPlugin'
    static final String EXTENSION_NAME = 'assembleTeamcityPluginConfig'


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.extensions.create ( EXTENSION_NAME, TeamCityExtension )

        final assemblePluginTask = project.tasks.add ( TASK_NAME, TeamCityTask )
        final tasks              = project.tasks.asMap
        final jarTask            = tasks[ 'jar'   ]
        final testTask           = tasks[ 'test'  ]
        final buildTask          = tasks[ 'build' ]

        if ( jarTask   ) { assemblePluginTask.dependsOn( jarTask.name   )}
        if ( testTask  ) { assemblePluginTask.dependsOn( testTask.name  )}
        if ( buildTask ) { buildTask.dependsOn( assemblePluginTask.name )}
    }
}
