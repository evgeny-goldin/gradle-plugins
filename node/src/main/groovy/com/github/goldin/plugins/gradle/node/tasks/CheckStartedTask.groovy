package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel


/**
 * Checks that Node.js application is up and running.
 */
class CheckStartedTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        delay( ext.checkWait * 1000 )

        final response       = httpRequest( ext.checkUrl, 'GET', [:], ext.checkTimeout * 500, ext.checkTimeout * 500, null, false )
        final content        = response.content ? new String( response.content, 'UTF-8' ) : ''
        final isGoodResponse = ( response.statusCode == ext.checkStatusCode ) && ( content.contains( ext.checkContent ))
        final resultMessage  = "Connecting to [$ext.checkUrl] resulted in " +
                               (( response.statusCode instanceof Integer ) ? "status code [$response.statusCode]" :
                                                                             "'$response.statusCode'" ) //  If not Integer then it's an error
        if ( isGoodResponse )
        {
            log{ "$resultMessage${ ext.checkContent ? ', content contains [' + ext.checkContent + ']' : '' } - good!" }
        }
        else
        {
            log( LogLevel.ERROR ) { 'The application has failed to start properly' }
            if ( ext.stopIfFailsToStart ){ runTask( STOP_TASK )}
            throw new GradleException( "$resultMessage${ ext.checkContent ? ', content [' + content + ']' : '' } " +
                                       "while we expected status code [$ext.checkStatusCode]" +
                                       ( ext.checkContent ? ", content contains [$ext.checkContent]" : '' ))
        }
    }
}
