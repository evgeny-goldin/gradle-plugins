package com.github.goldin.plugins.gradle.teamcity

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 *
 */
class AssembleTeamCityPlugin implements Plugin<Project>
{

    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( 'assembleTeamCityPlugin', AssembleTeamCityPluginTask )
        project.extensions.create( 'assembleTeamCityPlugin', AssembleTeamCityPluginExtension )
    }
}
