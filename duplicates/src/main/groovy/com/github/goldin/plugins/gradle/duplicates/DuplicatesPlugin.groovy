package com.github.goldin.plugins.gradle.duplicates

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Plugin that finds duplicate libraries in the scope specified.
 */
class DuplicatesPlugin implements Plugin<Project>
{
    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( 'duplicates', DuplicatesTask )
        project.extensions.create( 'duplicates', DuplicatesExtension )
    }
}
