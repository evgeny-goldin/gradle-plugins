package com.github.goldin.plugins.gradle.gitdump

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gradle.api.Project


/**
 * Gradle plugin for Git repositories backup.
 */
class GitDumpPlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks ( Project p ) {[ 'gitdump' : GitDumpTask ]}

    @Override
    Map<String , Class> extensions( Project p ) {[ 'gitdump' : GitDumpExtension ]}
}
