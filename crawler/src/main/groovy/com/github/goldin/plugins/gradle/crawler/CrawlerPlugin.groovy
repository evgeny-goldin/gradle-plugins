package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Gradle links crawler plugin.
 */
class CrawlerPlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks () {[ 'crawler' : CrawlerTask ]}

    @Override
    Map<String , Class> extensions() {[ 'crawler' : CrawlerExtension ]}
}
