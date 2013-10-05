package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BasePlugin
import org.gradle.api.Project


/**
 * Gradle links crawler plugin.
 */
class CrawlerPlugin extends BasePlugin
{
    @Override
    Map<String , Class<CrawlerTask>> tasks ( Project project ) {[ 'crawler' : CrawlerTask ]}

    @Override
    Map<String, Class<CrawlerExtension>> extensions( Project project ) {[ 'crawler' : CrawlerExtension ]}
}
