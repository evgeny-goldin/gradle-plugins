package com.github.goldin.plugins.gradle.common.helper

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Base helpers class connecting them to {@link BaseTask} methods.
 */
@SuppressWarnings([ 'AbstractClassWithoutAbstractMethod' ])
abstract class BaseHelper
{
    @Delegate final BaseTask task

    @Requires({ task })
    @Ensures ({ this.task })
    protected BaseHelper ( BaseTask task )
    {
        this.task = task
    }
}
