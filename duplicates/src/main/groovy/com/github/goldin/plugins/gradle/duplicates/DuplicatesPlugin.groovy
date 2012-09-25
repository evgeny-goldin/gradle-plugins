package com.github.goldin.plugins.gradle.duplicates

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Plugin that finds duplicate libraries in the scope specified.
 */
class DuplicatesPlugin implements Plugin<Project>
{
    static final String TASK_NAME      = 'duplicates'
    static final String EXTENSION_NAME = 'duplicates'


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( TASK_NAME,      DuplicatesTask )
        project.extensions.create( EXTENSION_NAME, DuplicatesExtension )
    }
}
