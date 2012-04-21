package com.github.goldin.plugins.gradle.about

import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin implements Plugin<Project>
{
    @Override
    void apply ( Project project )
    {
        project.tasks.add     ( 'about', AboutTask )
        project.extensions.add( 'about', new AboutPluginExtension())
    }
}
