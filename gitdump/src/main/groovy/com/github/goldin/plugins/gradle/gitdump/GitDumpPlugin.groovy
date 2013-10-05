package com.github.goldin.plugins.gradle.gitdump

import com.github.goldin.plugins.gradle.common.BasePlugin
import org.gradle.api.Project


/**
 * Gradle plugin for Git repositories backup.
 */
class GitDumpPlugin extends BasePlugin
{
    @Override
    Map<String , Class<GitDumpTask>> tasks ( Project project ) {[ 'gitdump' : GitDumpTask ]}

    @Override
    Map<String, Class<GitDumpExtension>> extensions( Project project ) {[ 'gitdump' : GitDumpExtension ]}
}
