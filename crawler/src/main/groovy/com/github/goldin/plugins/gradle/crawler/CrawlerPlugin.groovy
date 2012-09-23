package com.github.goldin.plugins.gradle.crawler

import org.gcontracts.annotations.Requires
import org.gradle.api.Plugin
import org.gradle.api.Project


/**
 * Gradle links crawler plugin.
 */
class CrawlerPlugin implements Plugin<Project>
{

    static final String TASK_NAME      = 'crawler'
    static final String EXTENSION_NAME = 'crawler'


    @Requires({ project })
    @Override
    void apply ( Project project )
    {
        project.tasks.add        ( TASK_NAME,      CrawlerTask      )
        project.extensions.create( EXTENSION_NAME, CrawlerExtension )
    }
}
