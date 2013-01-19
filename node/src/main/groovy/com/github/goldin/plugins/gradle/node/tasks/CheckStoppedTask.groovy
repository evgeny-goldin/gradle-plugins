package com.github.goldin.plugins.gradle.node.tasks

/**
 * Checks that Node.js application has stopped.
 */
class CheckStoppedTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        delay( ext.checkDelay )
        final response = httpRequest( ext.checkUrl, 'GET', [:], 0, 0, null, false )
        int j = 5
    }
}
