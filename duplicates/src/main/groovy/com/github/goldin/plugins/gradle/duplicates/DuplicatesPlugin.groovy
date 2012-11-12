package com.github.goldin.plugins.gradle.duplicates

import com.github.goldin.plugins.gradle.common.BasePlugin
import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Plugin that finds duplicate libraries in the scope specified.
 */
class DuplicatesPlugin extends BasePlugin
{
    @Override
    String extensionName() { 'duplicates' }

    @Override
    Class extensionClass (){ DuplicatesExtension }

    @Override
    String taskName() { 'duplicates' }

    @Override
    Class<? extends BaseTask> taskClass() { DuplicatesTask }
}
