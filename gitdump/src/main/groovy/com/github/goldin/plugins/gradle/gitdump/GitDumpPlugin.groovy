package com.github.goldin.plugins.gradle.gitdump

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Gradle links crawler plugin.
 */
class GitDumpPlugin implements Plugin<Project>
{

    static final String TASK_NAME      = 'gitdump'
    static final String EXTENSION_NAME = 'gitdump'


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( TASK_NAME,      GitDumpTask      )
        project.extensions.create( EXTENSION_NAME, GitDumpExtension )
    }
}
