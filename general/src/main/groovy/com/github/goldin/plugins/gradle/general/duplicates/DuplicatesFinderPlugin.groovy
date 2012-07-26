package com.github.goldin.plugins.gradle.general.duplicates

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin that finds duplicate libraries in the scope specified.
 */
class DuplicatesFinderPlugin implements Plugin<Project>
{
    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( 'duplicates', DuplicatesFinderTask )
        project.extensions.create( 'duplicates', DuplicatesFinderExtension )
    }
}
