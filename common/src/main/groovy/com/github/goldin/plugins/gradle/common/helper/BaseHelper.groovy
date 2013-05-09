package com.github.goldin.plugins.gradle.common.helper

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gradle.api.Project


/**
 * Base helpers class connecting them to {@link BaseTask} methods.
 */
@SuppressWarnings([ 'AbstractClassWithoutAbstractMethod' ])
abstract class BaseHelper<T>
{
    Project  project
    @Delegate
    BaseTask task
    T        ext
}
