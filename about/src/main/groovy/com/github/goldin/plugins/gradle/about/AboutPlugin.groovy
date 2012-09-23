package com.github.goldin.plugins.gradle.about

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
        project.tasks.add        ( 'about', AboutTask )
        project.extensions.create( 'about', AboutExtension )
    }
}
