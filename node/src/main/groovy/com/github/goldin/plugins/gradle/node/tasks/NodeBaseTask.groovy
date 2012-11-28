package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
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

        assert ext.stopCommands  || ext.scriptPath, "'stopCommands' or 'scriptPath' should be defined in $description"
        assert ext.startCommands || ext.scriptPath, "'startCommands' or 'scriptPath' should be defined in $description"

        ext.nodeVersion = ( ext.nodeVersion == 'latest' ) ? nodeHelper.latestNodeVersion() : ext.nodeVersion
        ext.isCoffee    = ext.scriptPath?.endsWith( '.coffee' )

        assert ( ext.isCoffee || ext.scriptPath?.endsWith( '.js' )), \
               "Unknown [$ext.scriptPath] - I only know '.coffee' and '.js' extensions"
    }


    @Requires({ project.buildDir && scriptName })
    @Ensures ({ result })
    final File scriptFile ( String scriptName ) { new File( project.buildDir, scriptName ) }


    /**
     * Retrieves base part of the bash script to be used by various tasks.
     */
    final String baseBashScript ()
    {
        final  binFolder = project.file( MODULES_BIN_DIR )
        assert ( binFolder.directory || ext.generateOnly ), "[$binFolder] is not available"

        """#!/bin/bash
        |
        |set -e
        |set -o pipefail
        |
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
     * Executes the script specified as bash command.
     *
     * @param scriptContent content to run as bash script
     * @param scriptFile    script file to create
     * @param failOnError   whether execution should fail if bash execution returns non-zero value
     * @param generateOnly  whether bash script should only be generated but not executed
     *
     * @return bash output or empty String if bash was generated but not executed
     */
    @Requires({ scriptContent && scriptFile })
    @Ensures ({ result != null })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    final String bashExec( String  scriptContent,
                           File    scriptFile,
                           boolean failOnError  = true,
                           boolean generateOnly = false )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        delete( scriptFile )

        scriptContent = ( ext.transformers ?: [] ).inject( scriptContent.trim() + '\n' ){
            String script, Closure c -> c( script, scriptFile, this ) ?: script
        }

        scriptFile.write( scriptContent, 'UTF-8' )
        assert scriptFile.with { file && size() }

        log( LogLevel.INFO ){ "Bash script created at [$scriptFile.canonicalPath], size [${ scriptFile.length() }] bytes" }

        if ( generateOnly ) { '' }
        else                { bashExec( scriptFile, project.rootDir, failOnError )}
    }
}
