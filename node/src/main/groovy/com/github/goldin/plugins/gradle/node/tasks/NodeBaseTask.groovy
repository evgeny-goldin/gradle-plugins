package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.node.ConfigHelper
import groovy.text.SimpleTemplateEngine
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.NodeHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.logging.LogLevel


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    final NodeHelper nodeHelper = new NodeHelper( logger )

    /**
     * Determines if current task requires an existence of {@link NodeExtension#scriptPath}
     * @return true if current task requires an existence of {@link NodeExtension#scriptPath},
     *         false otherwise
     */
    protected boolean requiresScriptPath(){ true }


    /**
     * Passes a new extensions object to the closure specified.
     * Registers new extension under task's name.
     */
    @Requires({ c })
    void config( Closure c )
    {
        this.extensionName = this.name
        this.ext           = project.extensions.create( this.extensionName, NodeExtension )
        c( this.ext )
    }


    @Requires({ ext.scriptPath })
    @Ensures ({ result != null })
    String executable()
    {
        final executable = ext.scriptPath.toLowerCase().endsWith( '.coffee' ) ? COFFEE_EXECUTABLE : ''
        if (  executable )
        {
            assert project.file( executable ).canonicalFile.file, "[$executable] is not available"
        }

        executable
    }


    @Override
    void verifyUpdateExtension ( String description )
    {
        assert project.name,            "'project.name' should be defined"
        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.nodeVersion,         "'nodeVersion' should be defined in $description"
        assert ext.testCommand,         "'testCommand' should be defined in $description"
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
        assert ext.portNumber > 0,      "'portNumber' should be positive in $description"

        ext.checkUrl   = ext.checkUrl   ?: "http://127.0.0.1:${ ext.portNumber }"
        ext.scriptPath = ext.scriptPath ?: ( new File( project.projectDir, 'server.js'     ).file ? 'server.js'     :
                                             new File( project.projectDir, 'server.coffee' ).file ? 'server.coffee' :
                                                                                                    null )
        assert ext.checkUrl
        assert ( ext.scriptPath || ( ! requiresScriptPath())), \
               "'scriptPath' should be defined in $description or use 'server.[js|coffee]' script to auto-discover it"

        ext.nodeVersion = ( ext.nodeVersion == 'latest' ) ? nodeHelper.latestNodeVersion() : ext.nodeVersion
        final addRedis  = (( ! ext.redisAddedAlready ) && (( ext.redisPort > 0 ) || ext.redisPortConfigKey ))
        if (  addRedis )
        {
            final redisPort    = ( ext.redisPort > 0 ) ? ext.redisPort as String : '${ config.' + ext.redisPortConfigKey + ' }'
            final redisRunning = '"`redis-cli -p ' + redisPort + ' ping 2> /dev/null`" = "PONG"'
            final getScript    = { String scriptName -> getResourceText( scriptName ).
                                                        replace( '${redisPort}',    redisPort ).
                                                        replace( '${redisRunning}', redisRunning ).
                                                        replace( '${sleep}',        ( ext.redisWait > 0 ? ext.redisWait : 0 ) as String )}
            ext.before            = getScript( 'redis-start.sh' ).readLines() + ( ext.before ?: [] )
            ext.after             = getScript( 'redis-stop.sh'  ).readLines() + ( ext.after  ?: [] )
            ext.redisAddedAlready = true
        }
    }


    @Requires({ project.buildDir && scriptName })
    @Ensures ({ result })
    final File scriptFile ( String scriptName ) { new File( project.buildDir, scriptName ) }


    @Requires({ taskName })
    final void runTask( String taskName )
    {
        log{ "Running task '$taskName'" }
        (( NodeBaseTask ) project.tasks[ taskName ] ).taskAction()
    }


    /**
     * Retrieves .pid file name to use when application is started and stopped.
     * @param port application port
     * @return .pid file name to use when application is started and stopped
     */
    @Requires({ port > 0 })
    @Ensures ({ result   })
    final String pidFileName( int port ){ "${ project.name }-${ port }.pid" }


    /**
     * Retrieves base part of the bash script to be used by various tasks.
     */
    @Requires({ operationTitle })
    final String baseBashScript ( String operationTitle )
    {
        final  binFolder = project.file( MODULES_BIN_DIR )
        assert binFolder.directory, "[$binFolder] is not available"
        final  q         = '"\\""'

        """
        |export NODE_ENV=$ext.NODE_ENV
        |export PATH=$binFolder:\$PATH
        |
        |. "\$HOME/.nvm/nvm.sh"
        |nvm use $ext.nodeVersion
        |
        |echo ---------------------------------------------
        |echo Running $q$operationTitle$q in $q`pwd`$q
        |echo \\\$NODE_ENV = $q$ext.NODE_ENV$q
        |echo ---------------------------------------------
        |
        """.stripMargin()
    }


    /**
     * Retrieves script content to be used as before/after execution interceptor.
     *
     * @param commands commands to execute
     * @return script content to be used as before/after execution interceptor.
     */
    @Requires({ commands })
    @Ensures ({ result })
    final String beforeAfterScript( List<String> commands )
    {
        if ( ext.configsResult == null )
        {
            ext.configsResult = readConfigs()
        }

        assert ( ext.configsResult != null )
        final Map binding = [ configs : ext.configsResult ] +
                            ( ext.configsResult ? [ config : ext.configsResult.head() ] : [:] )
        new SimpleTemplateEngine().createTemplate( commands.join( '\n' )).make( binding ).toString()
    }


    @Ensures ({ result != null })
    private List<Map<String, ?>> readConfigs ()
    {
        final result       = []
        final configHelper = new ConfigHelper( ext )

        for ( configMap in ( ext.configs ?: [] ))
        {
            configMap.each {
                String configPath, Object configValue ->
                result << ( configValue instanceof File ? configHelper.readConfigFile(( File ) configValue ) :
                            configValue instanceof Map  ? (( Map ) configValue ) :
                                                          [:] )
            }
        }

        result
    }


    /**
     * Executes the script specified as bash command.
     *
     * @param scriptContent  content to run as bash script
     * @param scriptFile     script file to create
     * @param watchExitCodes whether script exit codes need to be monitored (by adding set -e/set -o pipefail)
     * @param failOnError    whether execution should fail if bash execution results in non-zero value
     * @param useGradleExec  whether Gradle (true) or Ant (false) exec is used
     *
     * @return bash output or empty String if bash was generated but not executed or
     */
    @Requires({ scriptContent && scriptFile })
    @Ensures ({ result != null })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    final String bashExec( String  scriptContent,
                           File    scriptFile,
                           boolean watchExitCodes = true,
                           boolean failOnError    = true,
                           boolean useGradleExec  = true )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        delete( scriptFile )

        scriptContent = ( ext.transformers ?: [] ).inject(
        """#!/bin/bash
        |
        |${ watchExitCodes ? 'set -e'          : '' }
        |${ watchExitCodes ? 'set -o pipefail' : '' }
        |
        |${ scriptContent.readLines().join( '\n|' ) }
        |""".stripMargin()){
            String script, Closure transformer ->
            transformer( script, scriptFile, this ) ?: script
        }

        scriptFile.write( scriptContent, 'UTF-8' )
        assert scriptFile.with { file && size() }

        log( LogLevel.INFO ){ "Bash script created at [$scriptFile.canonicalPath], size [${ scriptFile.length() }] bytes" }

        if ( isLinux || isMac ) { exec( 'chmod', [ '+x', scriptFile.canonicalPath ]) }

        exec ( 'bash', [ scriptFile.canonicalPath ], project.projectDir, failOnError, useGradleExec )
    }
}
