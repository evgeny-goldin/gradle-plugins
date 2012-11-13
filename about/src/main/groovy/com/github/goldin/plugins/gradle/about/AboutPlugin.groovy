package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks () {[ 'about' : AboutTask ]}

    @Override
    Map<String , Class> extensions() {[ 'about' : AboutExtension ]}
}
