package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession


/**
 * Performs monitoring action.
 */
class MonitorTask extends BaseTask<MonitorExtension>
{
    // http://stackoverflow.com/questions/10258101/sslhandshakeexception-no-subject-alternative-names-present?answertab=active#tab-top
    static { HttpsURLConnection.defaultHostnameVerifier = { String hostname, SSLSession session -> true } as HostnameVerifier }


    @Override
    void verifyUpdateExtension ( String description )
    {
        assert ext.resources,           "'resources' should be defined in $description"
        assert ext.resources.file,      "[${ ext.resources }] is not available"
        assert ext.headers     != null, "'headers' should not be null in $description"
        assert ext.connectTimeout > -1, "'connectTimeout' should not be negative in $description"
        assert ext.readTimeout    > -1, "'readTimeout' should not be negative in $description"
    }


    @Requires({ ext.resources.file })
    @Override
    void taskAction()
    {
        final resourceLines = ext.resources.readLines()*.trim().grep().findAll { ! it.startsWith( '#' ) }
        final failures      = []
        final addFailure    = { String message -> failures << message; log( LogLevel.ERROR ){ ">>>> $message" }}

        for ( resourceLine in resourceLines )
        {
            def ( String checkUrl, String checkStatusCode, String checkContent, String timeLimit ) = resourceLine.tokenize( '|' )
            assert checkUrl
            checkStatusCode = checkStatusCode ?: '200'
            checkContent    = checkContent    ?: ''
            timeLimit       = timeLimit       ?: ( Long.MAX_VALUE as String )

            log { "Checking [$checkUrl], expecting status code [$checkStatusCode] and content containing [$checkContent]" }

            final response           = httpRequest( checkUrl, 'GET', ext.headers, ext.connectTimeout, ext.readTimeout, null, false, false )
            final responseStatusCode = response.statusCode.toString()
            final responseContent    = response.content ? new String( response.content, 'UTF-8' ) : ''
            final passed             = ( responseStatusCode == checkStatusCode ) && ( responseContent.contains( checkContent ))
            final timePassed         = ( response.timeMillis <= ( timeLimit as long ))

            if ( ! passed )
            {
                addFailure( "Requesting [$checkUrl] we received ${ response.statusCode instanceof Throwable ? 'error' : 'status code' } " +
                            "[$responseStatusCode] and content [$responseContent] " +
                            "while expected status code [$checkStatusCode] and content containing [$checkContent]" )
            }
            else if ( ! timePassed )
            {
                addFailure( "Requesting [$checkUrl] we received correct status code and content but it took " +
                            "[${ response.timeMillis }] ms while expected less than or equal to [${ timeLimit }] ms" )
            }

            log { "Checking [$checkUrl] - Ok, [${ response.timeMillis }] ms" }
        }

        if ( failures )
        {
            throw new GradleException( "The following checks have failed:\n* [${ failures.join( ']\n* [' )}]" )
        }
    }
}
