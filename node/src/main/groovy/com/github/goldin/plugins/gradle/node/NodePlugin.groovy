package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class NodePlugin extends BasePlugin
{
    @Override
    String extensionName() { 'node' }

    @Override
    Class extensionClass (){ NodeExtension }

    @Override
    String taskName() { 'node' }

    @Override
    Class<? extends BaseTask> taskClass() { NodeTask }
}
