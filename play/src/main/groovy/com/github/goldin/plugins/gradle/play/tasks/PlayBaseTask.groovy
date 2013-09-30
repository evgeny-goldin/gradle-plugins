package com.github.goldin.plugins.gradle.play.tasks

import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.play.PlayExtension


/**
 *
 */
abstract class PlayBaseTask extends BaseTask<PlayExtension>
{
    @Override
    Class extensionType (){ PlayExtension }


    @Override
    void verifyUpdateExtension ( String description )
    {
        assert ext.something
    }
}
