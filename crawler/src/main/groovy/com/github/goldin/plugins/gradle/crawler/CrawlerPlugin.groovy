package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Gradle links crawler plugin.
 */
class CrawlerPlugin extends BasePlugin
{
    @Override
    String extensionName() { 'crawler' }

    @Override
    Class extensionClass (){ CrawlerExtension }

    @Override
    String taskName() { 'crawler' }

    @Override
    Class<? extends BaseTask> taskClass() { CrawlerTask }
}
