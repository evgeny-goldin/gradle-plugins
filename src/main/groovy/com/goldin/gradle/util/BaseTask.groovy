package com.goldin.gradle.util

import com.goldin.gcommons.GCommons
import org.gradle.api.DefaultTask

/**
 * Base helper task class to be extended by other tasks
 */
class BaseTask extends DefaultTask
{

    public <T> T task ( String name, Class<T> type )
    {
        GCommons.verify().isInstance(( project.getTasksByName( name, true ) as List ).first(), type )
    }
}
