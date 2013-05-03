package com.github.goldin.plugins.gradle.common.helper

import com.github.goldin.plugins.gradle.common.BaseTask


/**
 * Base helpers class connecting them to {@link BaseTask} methods.
 */
@SuppressWarnings([ 'AbstractClassWithoutAbstractMethod', 'AbstractClassWithPublicConstructor' ])
abstract class BaseHelper
{
    @Delegate final BaseTask task

    BaseHelper ( BaseTask task )
    {
        this.task = task
    }
}
