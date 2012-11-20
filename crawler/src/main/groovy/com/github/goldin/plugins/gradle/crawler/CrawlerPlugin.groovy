package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gradle.api.Project


/**
 * Gradle links crawler plugin.
 */
class CrawlerPlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks ( Project p ) {[ 'crawler' : CrawlerTask ]}

    @Override
    Map<String , Class> extensions( Project p ) {[ 'crawler' : CrawlerExtension ]}
}
