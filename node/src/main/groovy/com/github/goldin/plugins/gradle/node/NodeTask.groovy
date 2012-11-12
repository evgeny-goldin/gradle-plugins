package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Executes Node.js commands.
 */
class NodeTask extends BaseTask
{
    NodeExtension ext(){ extension( NodeExtension ) }

    @Override
    void taskAction ( )
    {

    }
}
