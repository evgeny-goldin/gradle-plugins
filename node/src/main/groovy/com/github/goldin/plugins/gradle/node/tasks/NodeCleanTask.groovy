package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gradle.api.logging.LogLevel


/**
 * Cleans Node.js generated files.
 */
class NodeCleanTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        final deleteList = [ project.buildDir,
                             scriptFile( SETUP_SCRIPT ),
                             scriptFile( TEST_SCRIPT  ),
                             scriptFile( START_SCRIPT ),
                             new File( project.rootDir, NODE_MODULES_DIR )]

        log( LogLevel.INFO ){ "Deleting $deleteList" }
        project.delete( deleteList as Object[] )
    }
}
