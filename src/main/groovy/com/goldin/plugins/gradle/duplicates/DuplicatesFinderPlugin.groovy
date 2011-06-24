package com.goldin.plugins.gradle.duplicates

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin that finds duplicate libraries in the scope specified.
 */
class DuplicatesFinderPlugin implements Plugin<Project>
{
    @Override
    void apply ( Project p )
    {
        p.tasks.add( 'duplicates', DuplicatesFinderTask )
    }
}
