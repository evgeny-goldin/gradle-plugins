package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gradle.api.Project


/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks ( Project p ) {[ 'about' : AboutTask ]}

    @Override
    Map<String , Class> extensions( Project p ) {[ 'about' : AboutExtension ]}
}
