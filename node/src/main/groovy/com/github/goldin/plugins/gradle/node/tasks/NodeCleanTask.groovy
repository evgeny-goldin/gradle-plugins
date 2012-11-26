package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*


/**
 * Deletes Node.js generated files.
 */
class NodeCleanTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        delete( project.buildDir,
                scriptFile( SETUP_SCRIPT ),
                scriptFile( TEST_SCRIPT  ),
                scriptFile( START_SCRIPT ))
    }
}
