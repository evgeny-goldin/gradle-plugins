package com.goldin.plugins.gradle.util

import com.goldin.gcommons.GCommons
import com.goldin.gcommons.beans.VerifyBean
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip

/**
 * Base helper task class to be extended by other tasks
 */
abstract class BaseTask extends DefaultTask
{
    File       rootDir
    String     version
    Zip        zipTask
    VerifyBean verify = GCommons.verify()


    public <T> T task ( String name, Class<T> type )
    {
        verify.isInstance(( project.getTasksByName( name, true ) as List ).first(), type )
    }


    @TaskAction
    def doTask()
    {
        this.rootDir = verify.notNull( project.rootDir )
        this.group   = verify.notNull( project.group   )
        this.name    = verify.notNull( project.name    )
        this.version = verify.notNull( project.version )
        this.zipTask = task( 'jar', Zip )
        taskAction()
    }


    /**
     * Should be implemented by task.
     * Called after all fields are initialized.
     */
    abstract void taskAction()
}
