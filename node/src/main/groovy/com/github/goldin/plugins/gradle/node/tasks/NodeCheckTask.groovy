package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel


/**
 * Checks that Node.js application is up and running.
 */
class NodeCheckTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        delay( ext.checkDelay )

        final response       = httpRequest( ext.checkUrl, 'GET', [:], 0, 0, null, false )
        final content        = response.content ? new String( response.content, 'UTF-8' ) : ''
        final isGoodResponse = ( response.statusCode == ext.checkStatusCode ) && ( content.contains( ext.checkContent ))
        final resultMessage  = "Connecting to [$ext.checkUrl] resulted in " +
                               (( response.statusCode instanceof Integer ) ?
                                  "status code [$response.statusCode]" + ( ext.checkContent ? ", content [$content]" : '' ) :
                                  "'$response.statusCode'" ) //  If not Integer then it's an error
        if ( isGoodResponse )
        {
            log{ "$resultMessage - good!" }
        }
        else
        {
            if ( ext.stopIfFailsToStart )
            {
                log( LogLevel.ERROR ) { "The application has failed to start - running '$STOP_TASK' task" }
                (( NodeStopTask ) project.tasks[ STOP_TASK ] ).taskAction()
            }

            throw new GradleException( "$resultMessage and not as expected: status code [$ext.checkStatusCode]" +
                                       ( ext.checkContent ? ", content contains [$ext.checkContent]" : '' ))
        }
    }
}
