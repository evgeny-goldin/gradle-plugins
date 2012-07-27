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
        project.extensions.create ( ASSEMBLE_PLUGIN_EXTENSION, AssembleTeamCityPluginExtension )
        final assembleTask = project.tasks.add ( ASSEMBLE_PLUGIN_TASK, AssembleTeamCityPluginTask )
        final jarTask      = project.tasks[ 'jar' ]

        assert jarTask, "No 'jar' task is found in [$project]"
        assembleTask.dependsOn( jarTask.name )
    }
}
