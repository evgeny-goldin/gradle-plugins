package com.github.goldin.plugins.gradle.node.tasks


/**
 * Checks that Node.js application has stopped.
 */
class CheckStoppedTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        delay( ext.checkWait * 1000 )

        final response = httpRequest( ext.checkUrl, 'GET', [:], ext.checkTimeout * 500, ext.checkTimeout * 500, false, false )
        assert ( response.statusCode instanceof ConnectException ) &&
               ((( ConnectException ) response.statusCode ).message == 'Connection refused' ),
               "Connecting to [$ext.checkUrl] resulted in status code [${ response.statusCode }], expected ${ ConnectException.name }"
    }
}
