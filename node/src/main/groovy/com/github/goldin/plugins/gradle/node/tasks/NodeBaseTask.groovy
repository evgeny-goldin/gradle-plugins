package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.NodeHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


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
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
    }


    /**
     * Retrieves initial part of the bash script to be used by various tasks.
     */
    final String bashScript()
    {
        final  setupScript = new File( scriptPath( SETUP_SCRIPT ))
        assert setupScript.file, "[$setupScript] not found"

        final  binFolder   = new File( project.rootDir, NODE_MODULES_BIN )
        assert ( binFolder.directory || ext.generateOnly ), "[$binFolder] not found"

        """#!/bin/bash

        source $setupScript.canonicalPath
        export PATH=$binFolder:\$PATH

        """.stripIndent()
    }


    @Requires({ scriptName })
    @Ensures ({ result })
    final String scriptPath( String scriptName ) { "$project.buildDir/$scriptName" }
}
