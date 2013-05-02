package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BaseTask
import groovyx.gpars.GParsPool
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * Performs monitoring action.
 */
class MonitorTask extends BaseTask<MonitorExtension>
{
    @Override
    Class extensionType (){ MonitorExtension }


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
        final failures      = ext.runInParallel ? new ConcurrentLinkedQueue<String>() : []
        final resourceLines = (( ext.resourcesList ?: [] ) + ( ext.resourcesFile?.readLines() ?: [] ))*.
                              trim().grep().findAll { ! it.startsWith( '#' ) }

        assert resourceLines, 'No resources found to monitor'

        log { "Resources to monitor:\n* [${ resourceLines.join( ']\n* [' )}]\n\n" }

        final lineProcessor = {
            String line ->
            final failureMessage = processResourceLine( line )
            if ( failureMessage ) { failures << failureMessage; log( LogLevel.ERROR ){ ">>>> $failureMessage" }}
        }

        if ( ext.runInParallel ) { GParsPool.withPool { resourceLines.eachParallel { String line -> lineProcessor( line ) }}}
        else                     {                      resourceLines.each         { String line -> lineProcessor( line ) }}

        if ( failures )
        {
            throw new GradleException( "The following checks have failed:\n* [${ failures.join( ']\n* [' )}]" )
        }
    }


    @Requires({ line })
    String processResourceLine( String line )
    {
        def ( String title, String resource ) = line.contains( '=>' ) ? line.split( /\s*=>\s*/ ) : [ '', line ]

        final isHttpResource = resource.toLowerCase().with { startsWith( 'http://' ) || startsWith( 'https://' ) }
        final isNmapResource = resource.toLowerCase().startsWith( 'nmap://' )

        isHttpResource ? processHttpResource( title, resource ) :
        isNmapResource ? processNmapResource( title, resource ) :
                         ''
    }


    @Requires({ resource && resource.toLowerCase().with { startsWith( 'http://' ) || startsWith( 'https://' ) }})
    private String processHttpResource ( String title, String resource )
    {
        def ( String checkUrl, String checkStatusCode, String checkContent, String timeLimit, String user, String password ) = resource.tokenize( '|' )*.trim()

        assert checkUrl
        checkStatusCode = checkStatusCode ?: '200'
        checkContent    = checkContent    ?: ''
        timeLimit       = timeLimit       ?: ( Long.MAX_VALUE as String )
        final url       = "${ title ? "'$title' " : '' }[$checkUrl]"

        log { "$url - expecting status code [$checkStatusCode] and content matching [$checkContent]" }

        final response           = httpRequest( checkUrl, 'GET', ext.headers, ext.connectTimeout, ext.readTimeout, null, false, false, true,
                                                user ?: ext.user, password ?: ext.password )

        final responseStatusCode = response.statusCode.toString()
        final responseContent    = response.content ? new String( response.content, 'UTF-8' ) : ''
        final isMatch            = ( responseStatusCode == checkStatusCode ) &&
                                   contentMatches( responseContent, checkContent, ext.matchersDelimiter )
        final isTimeMatch        = ( response.timeMillis <= ( timeLimit as long ))

        log { "$url - [${ response.timeMillis }] ms" }

        if ( ! isMatch )
        {
            "Requesting $url we received ${ response.statusCode instanceof Throwable ? 'error' : 'status code' } " +
            "[$responseStatusCode] and content [$responseContent] " +
            "while we expected status code [$checkStatusCode] and content matching [$checkContent]"
        }
        else if ( ! isTimeMatch )
        {
            "Requesting $url we received correct status code and content but it took " +
            "[${ response.timeMillis }] ms while we expected less than or equal to [${ timeLimit }] ms"
        }
        else
        {
            ''
        }
    }


    @Requires({ resource && resource.toLowerCase().startsWith( 'nmap://' ) })
    private String processNmapResource ( String title, String resource )
    {
        def ( String address, String ports ) = resource.tokenize( '|' )*.trim()

        address         = address[ 'nmap://'.length() .. -1 ]
        final sortList  = { List<String> l -> l.collect { it as int }.toSet().sort() }
        final portsList = sortList( ports.tokenize( ',' )*.trim().grep())
        final url       = "${ title ? "'$title' " : '' }[nmap://$address]"

        assert address && portsList

        log { "$url - expecting open ports: $portsList" }

        final nmapOutput = exec( 'nmap', [ '-v', address ], null, true, true, LogLevel.DEBUG )
        final openPorts  = sortList( nmapOutput.readLines().findAll { String line -> line ==~ ~/\d+\/\w+\s+open\s+.+$/ }.
                                                            collect { String line -> line.find ( /(\d+)/ ){ it[ 1 ] }})

        log { "$url - found open ports: $openPorts" }

        if ( portsList != openPorts )
        {
            "Scanning $url for open ports we found $openPorts open ports while we expected $portsList"
        }
        else
        {
            ''
        }
    }
}
