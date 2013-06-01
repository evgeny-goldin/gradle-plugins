package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.BaseTask
import groovyx.gpars.GParsPool
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession
import java.text.SimpleDateFormat
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
        assert ext.resources,           "'resources' should be defined in $description"
        assert ext.headers     != null, "'headers' should not be null in $description"
        assert ext.connectTimeout > -1, "'connectTimeout' should not be negative in $description"
        assert ext.readTimeout    > -1, "'readTimeout' should not be negative in $description"
        assert ext.plotJsonFile,        "'plotJsonFile' should be defined in $description"
        ext.plotJsonFile = project.file( ext.plotJsonFile ) // From parent-less to project
    }


    @SuppressWarnings([ 'GroovyConditionalWithIdenticalBranches', 'GroovyAccessibility' ])
    @Override
    void taskAction()
    {
        final isPlot        = ( ext.plotBuilds > 0 )
        final isParallel    = ext.runInParallel
        final resourceLines =  (( ext.resources instanceof List ) ? ( List<String> ) ext.resources :
                                ( ext.resources instanceof File ) ? (( File ) ext.resources ).readLines()  :
                                                                    [] )*.trim().grep().findAll { ! it.startsWith( '#' ) }

        assert resourceLines, 'No \'resources\' found to monitor, please provide a List<String> or a File'
        assert (( ! isPlot ) || ( resourceLines.size() <= ArrayList.MAX_ARRAY_SIZE )), 'Wow, too many resources to monitor'

        final failures      = isParallel ? new ConcurrentLinkedQueue<String>() : []
        final resultsArray  = isPlot     ? new long[ resourceLines.size() ]    : null
        final urlsArray     = isPlot     ? new String[ resourceLines.size() ]  : null

        log { "Resources to monitor:\n* [${ resourceLines.join( ']\n* [' )}]\n\n" }

        final lineCallback = { String line, int index -> processResourceLine( line, index, isPlot, resultsArray, urlsArray, failures ) }
        if ( isParallel )    { GParsPool.withPool { resourceLines.eachWithIndexParallel( lineCallback )}}
        else                 {                      resourceLines.eachWithIndex( lineCallback )}

        if ( failures )
        {
            throw new GradleException( "The following checks have failed:\n* [${ failures.toSet().join( ']\n* [' )}]" )
        }
        else if ( isPlot )
        {
            createPlot( resultsArray, urlsArray )
        }
    }


    /**
     * Processes a single resource line and its result.
     */
    @Requires({ line && ( index > -1 ) && (( ! isPlot ) || ( resultsArray != null )) && ( failures != null ) })
    void processResourceLine ( String line, int index, boolean isPlot, long[] resultsArray, String[] urlsArray, Collection<String> failures )
    {
        def ( String title, String resource ) = line.contains( '=>' ) ? line.split( /\s*=>\s*/ ) : [ '', line ]

        final isHttpResource       = resource.toLowerCase().with { startsWith( 'http://' ) || startsWith( 'https://' ) }
        final isNmapResource       = resource.toLowerCase().startsWith( 'nmap://' )
        final isStatusCakeResource = resource.toLowerCase().startsWith( 'statuscake://' )

        /**
         * Result is of type List ([ HTTP response time, url ]) or String (failure message)
         */

        final result = isHttpResource       ? processHttpResource      ( title, resource ) :
                       isNmapResource       ? processNmapResource      ( title, resource ) :
                       isStatusCakeResource ? processStatusCakeResource( title, resource ) :
                                              ''
        if ( result instanceof List )
        {
            if ( isPlot ){ resultsArray[ index ] = result[ 0 ]; urlsArray[ index ] = result[ 1 ] }
        }
        else if ((( String ) result ).size() > 0 )
        {
            failures << result
            log( LogLevel.ERROR ){ ">>>> $result" }
        }
    }


    @Requires({ resource && resource.toLowerCase().with { startsWith( 'http://' ) || startsWith( 'https://' ) }})
    private Object processHttpResource ( String title, String resource )
    {
        def ( String checkUrl, String checkStatusCode, String checkContent, String timeLimit, String user, String password ) = resource.tokenize( '|' )*.trim()

        assert checkUrl
        checkStatusCode = checkStatusCode ?: '200'
        checkContent    = checkContent    ?: ''
        timeLimit       = timeLimit       ?: ( Long.MAX_VALUE as String )
        final url       = "${ title ? "'$title' " : '' }[$checkUrl]"

        log { "$url - expecting status code [$checkStatusCode] and content matching [$checkContent]" }

        final response           = httpRequest( checkUrl, 'GET', ext.headers, ext.connectTimeout, ext.readTimeout, false, false,
                                                user ?: ext.user, password ?: ext.password )
        final responseStatusCode = response.statusCode.toString()
        final responseContent    = response.asString()
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
            [ response.timeMillis, checkUrl ]
        }
    }


    @Requires({ resource && resource.toLowerCase().startsWith( 'nmap://' ) })
    private String processNmapResource ( String title, String resource )
    {
        def ( String address, String ports ) = resource[ 'nmap://'.length() .. -1 ].tokenize( '|' )*.trim()

        final sortList  = { List<String> l -> l.collect { it as int }.toSet().sort() }
        final portsList = sortList( ports.tokenize( ',' )*.trim().grep())
        final url       = "${ title ? "'$title' " : '' }[nmap://$address]"

        assert address && portsList

        log { "$url - expecting open ports: $portsList" }

        final nmapOutput = exec( 'nmap', [ '-v', address ], null, true, true, LogLevel.DEBUG )
        final openPorts  = sortList( nmapOutput.readLines().findAll { String line -> line ==~ ~/\d+\/\w+\s+open\s+.+$/ }.
                                                            collect { String line -> line.find ( /(\d+)/ ){ it[ 1 ] }})

        log { "$url - found open ports: $openPorts" }

        ( portsList != openPorts ) ? "Scanning $url for open ports we found $openPorts open ports while we expected $portsList" :
                                     ''
    }


    @Requires({ resource && resource.toLowerCase().startsWith( 'statuscake://' ) })
    private String processStatusCakeResource ( String title, String resource )
    {
        final list = resource[ 'statuscake://'.length() .. -1 ].tokenize( '|' )*.trim()
        final size = list.size()

        assert ( size == 2 ) || ( size == 3 ), "StatusCake resource [$resource] should contain 2 or 3 elements"

        final testName = ( size == 3 ) ? list.first() : ''
        final username = list[ -2 ]
        final apiKey   = list[ -1 ]
        final line     = "${ title ? "'$title' " : '' }[statuscake://${ testName ?: 'all tests' }]"

        final read     = {
            String url ->
            httpRequest( "https://www.statuscake.com/API/$url", 'GET', [ API : apiKey, Username : username ]).
            asString()
        }

        log { "$line - expecting ${ testName ? testName + ' test' : 'all tests' } to be up" }

        // http://kb.statuscake.com/api/The%20Basics/Authentication.md

        final  authMap = jsonToMap( read ( 'Auth' ))
        assert checkType( authMap.Success, Boolean ) && checkType((( Map ) authMap.Details ).Username, String )
        log { "$line - logged in successfully as '${ (( Map ) authMap.Details ).Username }'" }

        // http://kb.statuscake.com/api/Tests/Get%20All%20Tests.md

        final tests       = jsonToList( read( 'Tests' ), Map ).findAll { Map m -> ( testName ? m.WebsiteName == testName : true ) }
        final failedTests = tests.findAll{
            Map m ->
            (   checkType( m.Status, String  ) != 'Up' ) &&
            ( ! checkType( m.Paused, Boolean ))
        }

        log { "$line - found [${ tests.size() }] test${ s( tests )}, [${ failedTests.size() }] failed" }

        failedTests ? "StatusCake failed tests: ${ failedTests.collect{ Map m -> m.WebsiteName }}" :
                      ''
    }


    @Requires({ ( ext.plotJsonFile ) && ( ext.plotBuilds > 0 ) && resultsArray && urlsArray && ( resultsArray.size() == urlsArray.size()) })
    private void createPlot ( long[] resultsArray, String[] urlsArray )
    {
        final buildLabel  = systemEnv.BUILD_NUMBER ?: new SimpleDateFormat( 'dd-MM.HH-mm-ss' ).format( new Date( startTime ))
        final buildId     = buildLabel.replace( '.', '-' ).replace( '#', '-' )
        final buildUrl    = systemEnv.BUILD_URL    ?: teamCityBuildUrl ?: ''
        final plotFile    = ext.plotFile           ?: new File( project.buildDir , 'reports/plot.html' )
        final plotDataMap = ext.plotJsonFile.file  ?  jsonToMap( ext.plotJsonFile ) : [:]
        final results     = []
        final urls        = []
        final dataPoints  = []

        urlsArray.eachWithIndex {
            String url, int index ->
            // Getting rid of null entries caused by non-HTTP resources (like StatusCake) in resources being monitored.
            if ( url ) {
                results << resultsArray[ index ]
                urls    << url
            }
        }

        results.eachWithIndex { long result, int index -> dataPoints << [ index + 1, result ] }

        assert ( ! plotDataMap.containsKey( buildId )), "Build [$buildId] - plot data map $plotDataMap already contains this key"
        plotDataMap[ buildId ] = [ id: buildId, index: plotDataMap.size(), label: buildLabel, url: buildUrl, data: dataPoints ]

        if ( plotDataMap.size() > ext.plotBuilds )
        {
            final keys = plotDataMap.keySet()
            ( keys - keys.sort()[ -ext.plotBuilds .. -1 ] ).each { plotDataMap.remove( it ) }
        }

        write( plotFile, getResourceText( 'plot-template.html',
        [   // Map => Json
            datasets  : objectToJson( plotDataMap, ext.plotJsonFile ),
            // Java list => JavaScript array, element at index zero is an empty String
            urlsArray : "[\"\", \"${ urls.join( '","' ) }\"]"
        ]))

        println( "file:${ plotFile.canonicalPath } created" )
    }
}
