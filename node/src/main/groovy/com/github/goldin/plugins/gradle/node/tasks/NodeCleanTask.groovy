package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*


/**
 * Cleans Node.js generated files.
 */
class NodeCleanTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        project.delete( project.buildDir,
                        scriptPath( SETUP_SCRIPT ),
                        scriptPath( TEST_SCRIPT ),
                        scriptPath( START_SCRIPT ),
                        new File( project.rootDir, NODE_MODULES_DIR ))
    }
}
