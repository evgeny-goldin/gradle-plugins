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
        final response = httpRequest( ext.checkUrl, 'GET', [:], 0, 0, null, false, false )
        assert ( response.statusCode instanceof ConnectException ) &&
               ((( ConnectException ) response.statusCode ).message == 'Connection refused' ),
               "Connecting to [$ext.checkUrl] resulted in status code [${ response.statusCode }], expected ${ ConnectException.name }"
    }
}
