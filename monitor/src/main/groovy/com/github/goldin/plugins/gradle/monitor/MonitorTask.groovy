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
        assert ext.resources, "'resources' should be defined in $description"
        assert ext.resources.file, "[${ ext.resources }] is not available"
    }


    @Requires({ ext.resources.file })
    @Override
    void taskAction()
    {
        final resourceLines = ext.resources.readLines()*.trim().grep().findAll { ! it.startsWith( '#' ) }
        final failures      = []

        for ( resourceLine in resourceLines )
        {
            def ( String checkUrl, String checkStatusCode, String checkContent ) = resourceLine.tokenize( '|' )
            assert checkUrl
            checkStatusCode = checkStatusCode ?: '200'
            checkContent    = checkContent    ?: ''

            log { "Checking [$checkUrl], expecting status code [$checkStatusCode] and content containing [$checkContent]" }

            final response           = httpRequest( checkUrl, 'GET', [:], 0, 0, null, false )
            final responseStatusCode = response.statusCode.toString()
            final responseContent    = response.content ? new String( response.content, 'UTF-8' ) : ''
            final passed             = ( responseStatusCode == checkStatusCode ) && ( responseContent.contains( checkContent ))

            if ( ! passed )
            {
                final message = "Requesting [$checkUrl] received status code [$responseStatusCode] and content [$responseContent] " +
                                "while expected status code [$checkStatusCode] and content containing [$checkContent]"

                failures << message
                log( LogLevel.ERROR ){ ">> $message" }
            }
        }

        if ( failures )
        {
            throw new GradleException( "The following checks have failed:\n* [${ failures.join( ']\n* [' )}]" )
        }
    }
}
