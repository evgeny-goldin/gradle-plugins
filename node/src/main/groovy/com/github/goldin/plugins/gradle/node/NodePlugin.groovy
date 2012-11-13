package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class NodePlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks () {[ 'nodeTest' : NodeTestTask ]}

    @Override
    Map<String , Class> extensions() {[ 'node' : NodeExtension ]}
}
