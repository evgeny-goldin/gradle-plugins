package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession
import java.util.regex.Pattern


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
        assert ( ext.resourcesList || ext.resourcesFile ), "'resourcesList' or 'resourcesFile' should be defined in $description"
        assert ( ! ext.resourcesFile ) || ( ext.resourcesFile.file ), "[${ ext.resourcesFile }] is not available"
        assert ext.headers     != null, "'headers' should not be null in $description"
        assert ext.connectTimeout > -1, "'connectTimeout' should not be negative in $description"
        assert ext.readTimeout    > -1, "'readTimeout' should not be negative in $description"
    }


    @Requires({ ext.resourcesList || ext.resourcesFile?.file })
    @Override
    void taskAction()
    {
        final resourceLines = (( ext.resourcesList ?: [] ) + ( ext.resourcesFile?.readLines() ?: [] ))*.
                              trim().grep().findAll { ! it.startsWith( '#' ) }
        final failures      = []
        final addFailure    = { String message -> failures << message; log( LogLevel.ERROR ){ ">>>> $message" }}

        assert resourceLines, 'No resources found to monitor'

        for ( resourceLine in resourceLines )
        {
            def ( String checkUrl, String checkStatusCode, String checkContent, String timeLimit ) = resourceLine.tokenize( '|' )
            assert checkUrl
            checkStatusCode = checkStatusCode ?: '200'
            checkContent    = checkContent    ?: ''
            timeLimit       = timeLimit       ?: ( Long.MAX_VALUE as String )

            log { "[$checkUrl] - expecting status code [$checkStatusCode] and content matching [$checkContent]" }

            final response           = httpRequest( checkUrl, 'GET', ext.headers, ext.connectTimeout, ext.readTimeout, null, false, false )
            final responseStatusCode = response.statusCode.toString()
            final responseContent    = response.content ? new String( response.content, 'UTF-8' ) : ''
            final isMatch            = ( responseStatusCode == checkStatusCode ) && contentMatches( responseContent, checkContent )
            final isTimeMatch        = ( response.timeMillis <= ( timeLimit as long ))

            if ( ! isMatch )
            {
                addFailure( "Requesting [$checkUrl] we received ${ response.statusCode instanceof Throwable ? 'error' : 'status code' } " +
                            "[$responseStatusCode] and content [$responseContent] " +
                            "while we expected status code [$checkStatusCode] and content matching [$checkContent]" )
            }
            else if ( ! isTimeMatch )
            {
                addFailure( "Requesting [$checkUrl] we received correct status code and content but it took " +
                            "[${ response.timeMillis }] ms while we expected less than or equal to [${ timeLimit }] ms" )
            }

            log { "[$checkUrl] - [${ response.timeMillis }] ms" }
        }

        if ( failures )
        {
            throw new GradleException( "The following checks have failed:\n* [${ failures.join( ']\n* [' )}]" )
        }
    }


    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    @Requires({ ( content != null ) && ( pattern != null ) })
    static boolean contentMatches( String content, String pattern )
    {
        final positiveMatch = ( ! pattern.startsWith( '-' ))
        pattern             = positiveMatch ? pattern : pattern[ 1 .. -1 ]
        final regexMatch    = pattern.with { startsWith( '/' ) && endsWith( '/' ) }
        pattern             = regexMatch    ? pattern[ 1 .. -2 ] : pattern
        final isMatch       = regexMatch    ? Pattern.compile ( pattern ).matcher( content ).find() :
                                              content.contains( pattern )

        ( positiveMatch ? isMatch : ( ! isMatch ))
    }
}