package com.github.goldin.plugins.gradle.common

import com.github.goldin.plugins.gradle.common.extensions.BaseExtension
import com.github.goldin.plugins.gradle.common.helpers.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.text.SimpleDateFormat


/**
 * Base helper task class to be extended by other tasks
 */
abstract class BaseTask<T extends BaseExtension> extends DefaultTask
{
    final SimpleDateFormat dateFormatter      = new SimpleDateFormat( 'dd MMM, EEEE, yyyy, HH:mm:ss (zzzzzz:\'GMT\'ZZZZZZ)', Locale.ENGLISH )
    final long             startTime          = System.currentTimeMillis()
    final String           startTimeFormatted = this.dateFormatter.format( new Date( this.startTime ))
    final String           osName             = System.getProperty( 'os.name', 'unknown' ).toLowerCase()
    final boolean          isWindows          = osName.contains( 'windows' )
    final boolean          isLinux            = osName.contains( 'linux'   )
    final boolean          isMac              = osName.contains( 'mac os'  )
    final String           projectName        = project.name.replaceAll( ~/^.*\//, '' )
    final File             projectDir         = project.projectDir

    final Map<String,String> systemEnv        = System.getenv().asImmutable()
    final Map<String,String> systemProperties = ( Map<String,String> ) System.properties.asImmutable()

    @Delegate GeneralHelper  generalHelper
    @Delegate IOHelper       ioHelper
    @Delegate JsonHelper     jsonHelper
    @Delegate MatcherHelper  matcherHelper
    @Delegate TeamCityHelper teamCityHelper

    /**
     * Retrieves task's extension type in run-time
     */
    @Ensures ({ result })
    abstract Class<T> extensionType()

    /**
     * Extension instance and its name are set by {@link BasePlugin#addTask}
     */
    String extensionName
    T      ext

    /**
     * Configuration closure allowing to configure and use task-named extension when task is called.
     * This allows to
     * 1) Configure the task lazily so if any computations are involved they're not executed unless the task is called.
     * 2) Use several tasks named differently each one configuring its own extension.
     */
    Closure config
    @Requires({ c })
    void config( Closure c ){ this.config = c }

    @Requires({ this.ext && description })
    abstract void verifyUpdateExtension ( String description )

    @Requires({ ext && extensionName })
    abstract void taskAction()


    @TaskAction
    final void doTask()
    {
        assert project.name && this.name

        if ( this.config )
        {
            this.extensionName = this.name
            this.ext           = project.extensions.create( this.extensionName, extensionType())
            this.config( this.ext )
        }

        assert this.ext && this.extensionName

        generalHelper  = new GeneralHelper ( this.project, this, this.ext )
        ioHelper       = new IOHelper      ( this.project, this, this.ext )
        jsonHelper     = new JsonHelper    ( this.project, this, this.ext )
        matcherHelper  = new MatcherHelper ( this.project, this, this.ext )
        teamCityHelper = new TeamCityHelper( this.project, this, this.ext )

        verifyUpdateExtension( "$project => ${ this.extensionName } { .. }" )
        taskAction()
    }
}
