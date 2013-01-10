package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
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
    final NodeHelper nodeHelper = new NodeHelper()


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


    @Override
    void verifyUpdateExtension ( String description )
    {
        assert project.name,            "'project.name' should be defined"
        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.nodeVersion,         "'nodeVersion' should be defined in $description"
        assert ext.testCommand,         "'testCommand' should be defined in $description"
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
        assert ext.checkUrl,            "'checkUrl' should be defined in $description"

        assert ext.stopCommands  || ext.scriptPath, "'stopCommands' or 'scriptPath' should be defined in $description"
        assert ext.startCommands || ext.scriptPath, "'startCommands' or 'scriptPath' should be defined in $description"

        ext.nodeVersion = ( ext.nodeVersion == 'latest' ) ? nodeHelper.latestNodeVersion() : ext.nodeVersion
        final addRedis  = (( ! ext.redisAdded ) && (( ext.redisPort > 0 ) || ext.redisPortConfigKey ))
        if (  addRedis )
        {
            final redisPort    = ( ext.redisPort > 0 ) ? ext.redisPort as String : '${ config.' + ext.redisPortConfigKey + ' }'
            final jenkinsSpawn = 'export BUILD_ID=JenkinsLetMeSpawn'
            final stopRedis    = "redis-cli -p $redisPort shutdown"
            final startRedis   = "redis-server --port $redisPort &"
            ext.before         = [ jenkinsSpawn, stopRedis, startRedis ] + ( ext.before ?: [] )
            ext.after          = [ stopRedis ] + ( ext.after  ?: [] )
            ext.redisAdded     = true
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
    final String baseBashScript ()
    {
        final  binFolder = project.file( MODULES_BIN_DIR )
        assert binFolder.directory, "[$binFolder] is not available"

        """
        |export NODE_ENV=$ext.NODE_ENV
        |export PATH=$binFolder:\$PATH
        |
        |. "\$HOME/.nvm/nvm.sh"
        |nvm use $ext.nodeVersion
        |
        |echo ---------------------------------------------
        |echo \\\$NODE_ENV = [$ext.NODE_ENV]
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
        final Map binding = [ configs : ext.configsResult ] +
                            ( ext.configsResult ? [ config : ext.configsResult.head() ] : [:] )
        new SimpleTemplateEngine().createTemplate( commands.join( '\n' )).make( binding ).toString()
    }


    /**
     * Executes the script specified as bash command.
     *
     * @param scriptContent content to run as bash script
     * @param scriptFile    script file to create
     * @param failOnError   whether execution should fail if bash execution returns non-zero value
     * @param useGradleExec whether Gradle (true) or Ant (false) exec is used
     *
     * @return bash output or empty String if bash was generated but not executed or
     */
    @Requires({ scriptContent && scriptFile })
    @Ensures ({ result != null })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    final String bashExec( String  scriptContent,
                           File    scriptFile,
                           boolean failOnError   = true,
                           boolean useGradleExec = true )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        delete( scriptFile )

        scriptContent = ( ext.transformers ?: [] ).inject(
        """#!/bin/bash
        |
        |${ failOnError ? 'set -e'          : '' }
        |${ failOnError ? 'set -o pipefail' : '' }
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

        exec ( 'bash', [ scriptFile.canonicalPath ], project.rootDir, failOnError, useGradleExec )
    }
}
