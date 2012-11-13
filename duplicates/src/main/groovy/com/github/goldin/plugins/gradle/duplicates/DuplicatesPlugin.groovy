package com.github.goldin.plugins.gradle.duplicates

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Plugin that finds duplicate libraries in the scope specified.
 */
class DuplicatesPlugin extends BasePlugin
{
    @Override
    Map<String , Class<? extends BaseTask>> tasks () {[ 'duplicates' : DuplicatesTask ]}

    @Override
    Map<String , Class> extensions() {[ 'duplicates' : DuplicatesExtension ]}
}
