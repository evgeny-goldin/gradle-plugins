package com.github.goldin.plugins.gradle.node.tasks


/**
 * Checks that Node.js application has stopped.
 */
class CheckStoppedTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        ext.checks.each {
            String checkUrl, List<?> list ->

            final response           = httpRequest( checkUrl, 'GET', [:], ext.checkTimeout * 500, ext.checkTimeout * 500, false, false )
            final responseStatusCode = response.statusCode
            assert ( responseStatusCode instanceof ConnectException ) &&
                   ((( ConnectException ) responseStatusCode ).message == 'Connection refused' ),
                   "Connecting to [$checkUrl] resulted in status code [$responseStatusCode], expected ${ ConnectException.name }"
        }
    }
}
