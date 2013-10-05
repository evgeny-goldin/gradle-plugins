package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.BasePlugin
import org.gradle.api.Project


/**
 * Plugin that adds "about" build metadata to build artifacts.
 */
class AboutPlugin extends BasePlugin
{
    @Override
    Map<String , Class<AboutTask>> tasks ( Project project ) {[ 'about' : AboutTask ]}

    @Override
    Map<String, Class<AboutExtension>> extensions( Project project ) {[ 'about' : AboutExtension ]}
}
