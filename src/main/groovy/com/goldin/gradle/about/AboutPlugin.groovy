package com.goldin.gradle.about

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
        p.task( 'about' ) << {
            println "Generating about"
        }
    }
}
