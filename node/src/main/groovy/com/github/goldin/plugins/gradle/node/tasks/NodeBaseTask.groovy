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
    final NodeHelper helper = new NodeHelper()


    @Override
    void verifyExtension( String description )
    {
        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.nodeVersion,         "'nodeVersion' should be defined in $description"
        assert ext.testCommand,         "'testCommand' should be defined in $description"
        assert ext.stopCommand,         "'stopCommand' should be defined in $description"
        assert ext.startCommand,        "'startCommand' should be defined in $description"
        assert ext.listCommand,         "'listCommand' should be defined in $description"
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
    }


    @Requires({ project.buildDir && scriptName })
    @Ensures ({ result })
    final File scriptFile ( String scriptName ) { new File( project.buildDir, scriptName ) }


    /**
     * Retrieves initial part of the bash script to be used by various tasks.
     */
    final String bashScript()
    {
        final  setupScript = scriptFile( SETUP_SCRIPT )
        assert setupScript.file, "[$setupScript] not found"

        final  binFolder   = new File( project.rootDir, NODE_MODULES_BIN )
        assert ( binFolder.directory || ext.generateOnly ), "[$binFolder] not found"

        """#!/bin/bash
        |
        |source \${0%/*}/$SETUP_SCRIPT
        |
        |export PATH=$binFolder:\$PATH
        |export NODE_ENV=$ext.NODE_ENV""".stripMargin()
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
                           boolean failOnError    = true,
                           boolean generateOnly   = false )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        assert scriptFile.with            { ( ! file ) || project.delete( delegate ) }, "Failed to delete [$scriptFile.canonicalPath]"

        scriptContent = ( ext.transformers ?: [] ).inject( scriptContent ){
            String script, Closure c -> c( script, scriptFile, this )
        }

        scriptFile.write( scriptContent, 'UTF-8' )
        assert scriptFile.with { file && size() }

        log( LogLevel.INFO ){ "Bash script created at [$scriptFile.canonicalPath], size [${ scriptFile.length() }] bytes" }

        if ( generateOnly ) { '' }
        else                { bashExec( scriptFile, project.rootDir, failOnError )}
    }
}
