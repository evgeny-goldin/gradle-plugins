package com.github.goldin.plugins.gradle.common.helper

import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Base helpers class connecting them to {@link BaseTask} methods.
 */
@SuppressWarnings([ 'AbstractClassWithoutAbstractMethod' ])
abstract class BaseHelper
{
    @Delegate final BaseTask task

    protected BaseHelper ( BaseTask task /* null in tests */ )
    {
        this.task = task
    }
}
