package com.github.goldin.plugins.gradle.common.helpers

import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.extensions.BaseExtension
import org.gcontracts.annotations.Ensures
import org.gradle.api.Project


/**
 * Base helpers class connecting them to {@link BaseTask} methods.
 */
@SuppressWarnings([ 'AbstractClassWithoutAbstractMethod' ])
abstract class BaseHelper<T extends BaseExtension>
{
    final Project  project
    @Delegate
    final BaseTask task
    final T        ext


    @Ensures({ result })
    File buildDir(){ project.buildDir }


    @SuppressWarnings([ 'GrFinalVariableAccess' ])
    protected BaseHelper ( Project project, BaseTask task, T ext )
    {
        this.@project = project
        this.@task    = task
        this.@ext     = ext
    }
}
