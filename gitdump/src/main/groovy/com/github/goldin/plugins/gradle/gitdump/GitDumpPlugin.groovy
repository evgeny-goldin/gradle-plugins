package com.github.goldin.plugins.gradle.gitdump

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Gradle plugin for Git repositories backup.
 */
class GitDumpPlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks () {[ 'gitdump' : GitDumpTask ]}

    @Override
    Map<String , Class> extensions() {[ 'gitdump' : GitDumpExtension ]}
}
