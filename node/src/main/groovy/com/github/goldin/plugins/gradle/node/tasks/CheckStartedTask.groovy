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

        final response       = httpRequest( ext.checkUrl, 'GET', [:], ext.checkTimeout * 500, ext.checkTimeout * 500, false )
        final content        = response.content ? response.contentAsString() : ''
        final isGoodResponse = ( response.statusCode == ext.checkStatusCode ) && contentMatches( content, ext.checkContent, '*' )
        final resultMessage  = "Connecting to [$ext.checkUrl] resulted in " +
                               (( response.statusCode instanceof Integer ) ? "status code [$response.statusCode]" :
                                                                             "'$response.statusCode'" ) //  If not Integer then it's an error
        if ( isGoodResponse )
        {
            log{ "$resultMessage${ ext.checkContent ? ', content contains [' + ext.checkContent + ']' : '' } - good!" }
        }
        else
        {
            log( LogLevel.ERROR ) { '''
                                    |-----------------------------------------------------------
                                    |  -=-= The application has failed to start properly! =-=-
                                    |-----------------------------------------------------------
                                    '''.stripMargin() }

            shellExec( tailLogScript(), taskScriptFile( false, false, 'tail-log' ), false, true, false, true, 'display application log', LogLevel.ERROR )
            if ( ext.stopIfFailsToStart ){ runTask( STOP_TASK )}
            throw new GradleException( "$resultMessage${ ext.checkContent ? ', content [' + content + ']' : '' } " +
                                       "while we expected status code [$ext.checkStatusCode]" +
                                       ( ext.checkContent ? ", content contains [$ext.checkContent]" : '' ) +
                                       ". See 'Showing logs' above." )
        }
    }


    @SuppressWarnings([ 'LineLength' ])
    String tailLogScript()
    {
        // Sorting "forever list" output by processes uptime, taking first element with a minimal uptime and listing its log.
        """
        |echo $LOG_DELIMITER
        |${ forever() } logs `${ forever() } list --plain | $REMOVE_COLOR_CODES | grep -E '\\[[0-9]+\\]' | awk '{print \$NF,\$2}' | sort -n | head -1 | awk '{print \$2}' | tr -d '[]'`${ ext.removeColorCodes }
        |echo $LOG_DELIMITER
        """.stripMargin().toString().trim()
    }
}
