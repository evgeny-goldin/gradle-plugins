package com.github.goldin.plugins.gradle.general.about

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin implements Plugin<Project>
{
    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add     ( 'about', AboutTask )
        project.extensions.add( 'about', new AboutPluginExtension())
    }
}
