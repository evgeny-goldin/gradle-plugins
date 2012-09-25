package com.github.goldin.plugins.gradle.about

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin implements Plugin<Project>
{
    static final String TASK_NAME      = 'about'
    static final String EXTENSION_NAME = 'about'


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( TASK_NAME,      AboutTask )
        project.extensions.create( EXTENSION_NAME, AboutExtension )
    }
}
