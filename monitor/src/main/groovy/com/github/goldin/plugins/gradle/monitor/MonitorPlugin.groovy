package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Plugin for monitoring external resources.
 */
class MonitorPlugin extends BasePlugin
{
    @Requires({ project })
    @Override
    Map<String , Class<? extends BaseTask>> tasks ( Project project ){[ monitor : MonitorTask ]}

    @Override
    Map<String , Class> extensions( Project project ) {[ monitor : MonitorExtension ]}
}
