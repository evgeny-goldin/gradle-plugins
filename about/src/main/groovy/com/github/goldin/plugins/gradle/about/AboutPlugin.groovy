package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin extends BasePlugin
{
    @Override
    String extensionName() { 'about' }

    @Override
    Class extensionClass (){ AboutExtension }

    @Override
    String taskName() { 'about' }

    @Override
    Class<? extends BaseTask> taskClass() { AboutTask }
}
