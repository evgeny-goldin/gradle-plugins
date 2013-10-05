package com.github.goldin.plugins.gradle.duplicates

import com.github.goldin.plugins.gradle.common.BasePlugin
import org.gradle.api.Project


/**
 * Plugin that finds duplicate libraries in the scope specified.
 */
class DuplicatesPlugin extends BasePlugin
{
    @Override
    Map<String , Class<DuplicatesTask>> tasks ( Project project ) {[ 'duplicates' : DuplicatesTask ]}

    @Override
    Map<String, Class<DuplicatesExtension>> extensions( Project project ) {[ 'duplicates' : DuplicatesExtension ]}
}
