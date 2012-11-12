package com.github.goldin.plugins.gradle.gitdump

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Gradle plugin for Git repositories backup.
 */
class GitDumpPlugin extends BasePlugin
{
    @Override
    String extensionName() { 'gitdump' }

    @Override
    Class extensionClass (){ GitDumpExtension }

    @Override
    String taskName() { 'gitdump' }

    @Override
    Class<? extends BaseTask> taskClass() { GitDumpTask }
}
