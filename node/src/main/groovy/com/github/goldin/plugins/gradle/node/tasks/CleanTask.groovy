package com.github.goldin.plugins.gradle.node.tasks


/**
 * Deletes all Node.js generated files.
 */
class CleanTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        delete( buildDir())
    }
}
