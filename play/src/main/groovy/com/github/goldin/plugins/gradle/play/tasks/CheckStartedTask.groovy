package com.github.goldin.plugins.gradle.play.tasks

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel


/**
 * Checks that application has started.
 */
class CheckStartedTask extends PlayBaseTask
{
    @Override
    @Requires({ ext.checks })
    void taskAction()
    {
        sleepMs( ext.checkWait * 1000 )

        ext.checks.each {
            String checkUrl, List<?> list ->

            assert checkUrl && list && ( list.size() == 2 )

            final checkStatusCode    = list[ 0 ] as int
            final checkContent       = list[ 1 ] as String
            final response           = httpRequest( checkUrl, 'GET', [:], ext.checkTimeout * 500, ext.checkTimeout * 500, false )
            final responseStatusCode = response.statusCode
            final responseContent    = response.asString()
            final isGoodResponse     = ( responseStatusCode == checkStatusCode ) && contentMatches( responseContent, checkContent, '*' )
            final logMessage         = "Connecting to $checkUrl resulted in " +
                                       (( responseStatusCode instanceof Integer ) ? "status code [$responseStatusCode]" :
                                                                                    "'$responseStatusCode'" ) //  If not Integer then it's an error
            if ( isGoodResponse )
            {
                log{ "$logMessage${ checkContent ? ', content contains [' + checkContent + ']' : '' } - good!" }
            }
            else
            {
                final errorDetails   = "$logMessage, content [$responseContent] while we expected status code [$checkStatusCode]" +
                                       ( checkContent ? ", content contains [$checkContent]" : '' )
                final errorMessage   = """
                                       |-----------------------------------------------------------
                                       |  -=-= The application has failed to start properly! =-=-
                                       |-----------------------------------------------------------
                                       |$errorDetails
                                       """.stripMargin()

                log( LogLevel.ERROR ) { errorMessage }

                if ( ext.stopIfFailsToStart ){ runTask( STOP_TASK )}

                throw new GradleException( errorMessage )
            }
        }

        final  runningPidFile = project.file( RUNNING_PID )
        assert runningPidFile.file, "'${runningPidFile.canonicalPath}' is missing"
    }
}
