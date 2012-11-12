package com.github.goldin.plugins.gradle.node
import com.github.goldin.plugins.gradle.common.BasePlugin


/**
 * Plugin for building and packaging TeamCity plugins.
 */
class NodePlugin extends BasePlugin
{
    @Override
    String taskName() { 'node' }

    @Override
    Class taskClass() { NodeTask }

    @Override
    String extensionName() { 'node' }

    @Override
    Class extensionClass (){ NodeExtension }
}
