package com.github.goldin.plugins.gradle.gitdump

import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * {@link GitDumpPlugin} task.
 */
class GitDumpTask extends BaseTask
{
    private GitDumpExtension ext () { extension ( GitDumpPlugin.EXTENSION_NAME, GitDumpExtension ) }

    @Override
    void taskAction ( )
    {
        final ext = ext()
        for ( repoUrl in ext.urls )
        {
            logger.info( "Cloning [$repoUrl] .. " )
        }
    }
}
