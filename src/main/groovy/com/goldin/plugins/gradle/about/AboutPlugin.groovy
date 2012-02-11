package com.goldin.plugins.gradle.about

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin implements Plugin<Project>
{
    @Override
    void apply ( Project p )
    {
        p.tasks.add( 'about', AboutTask )
        p.convention.plugins[ 'about' ] = new AboutPluginConvention()
    }
}
