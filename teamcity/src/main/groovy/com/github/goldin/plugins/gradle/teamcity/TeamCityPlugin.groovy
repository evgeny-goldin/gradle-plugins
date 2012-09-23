package com.github.goldin.plugins.gradle.teamcity

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class TeamCityPlugin implements Plugin<Project>
{
    static final String ASSEMBLE_PLUGIN_TASK      = 'assembleTeamcityPlugin'
    static final String ASSEMBLE_PLUGIN_EXTENSION = 'assembleTeamcityPluginConfig'


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.extensions.create ( ASSEMBLE_PLUGIN_EXTENSION, TeamCityExtension )

        final assemblePluginTask = project.tasks.add ( ASSEMBLE_PLUGIN_TASK, TeamCityTask )
        final tasks              = project.tasks.asMap
        final jarTask            = tasks[ 'jar'   ]
        final testTask           = tasks[ 'test'  ]
        final buildTask          = tasks[ 'build' ]

        if ( jarTask   ) { assemblePluginTask.dependsOn( jarTask.name   )}
        if ( testTask  ) { assemblePluginTask.dependsOn( testTask.name  )}
        if ( buildTask ) { buildTask.dependsOn( assemblePluginTask.name )}
    }
}
