package com.github.goldin.plugins.gradle.crawler

import static com.github.goldin.plugins.gradle.crawler.CrawlerConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.HttpResponse
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong


/**
 * {@link CrawlerPlugin} task.
 */
class CrawlerTask extends BaseTask<CrawlerExtension>
{
    @Override
    Class extensionType (){ CrawlerExtension }


    private final Queue<Future> futures         = new ConcurrentLinkedQueue<Future>()
    private final AtomicLong    bytesDownloaded = new AtomicLong( 0L )
    private final AtomicLong    linksProcessed  = new AtomicLong( 0L )

    private volatile boolean            crawlingAborted = false // If ever set to true - crawling process is aborted immediately
    private          ThreadPoolExecutor threadPool
    private          LinksStorage       linksStorage


    /**
     * Verifies {@link CrawlerExtension} contains proper settings and updates it with additional properties.
     * @return {@link CrawlerExtension} instance verified and updated.
     */
    @Override
    void verifyUpdateExtension ( String description )
    {
        assert ( ! ext.rootUrl             ), "'rootUrl' should not be used in $description - private area"
        assert ( ! ext.internalLinkPattern ), "'internalLinkPattern' should not be used in $description - private area"

        ext.baseUrl             = ext.baseUrl?.trim()?.replace( '\\', '/' )?.replaceAll( '^.+?:/+', '' ) // Protocol part removed
        ext.rootUrl             = ext.baseUrl?.replaceAll( '/.*', '' )                                   // Path part removed
        ext.internalLinkPattern = ~/(?:('|")|>)(https?:\/\/\Q${ ext.baseUrl }\E.*?)(?:\1|<)/

        assert ext.baseUrl, "'baseUrl' should be defined in $description"
        assert ext.rootUrl && ( ! ext.rootUrl.endsWith( '/' )) && ext.internalLinkPattern

        assert ext.userAgent,                 "'userAgent' should be defined in $description"
        assert ext.threadPoolSize       >  0, "'threadPoolSize' [${ ext.threadPoolSize }] in $description should be positive"
        assert ext.checksumsChunkSize   >  0, "'checksumsChunkSize' [${ ext.checksumsChunkSize }] in $description should be positive"
        assert ext.futuresPollingPeriod >  0, "'futuresPollingPeriod' [${ ext.futuresPollingPeriod }] in $description should be positive"
        assert ext.connectTimeout       > -1, "'connectTimeout' [${ ext.connectTimeout }] in $description should not be negative"
        assert ext.readTimeout          > -1, "'readTimeout' [${ ext.readTimeout }] in $description should not be negative"
        assert ext.retries              > -1, "'retries' [${ ext.retries }] in $description should not be negative"
        assert ext.retryDelay           > -1, "'retryDelay' [${ ext.retryDelay }] in $description should not be negative"
        assert ext.requestDelay         > -1, "'requestDelay' [${ ext.requestDelay }] in $description should not be negative"

        assert ext.retryStatusCodes.every { it }, "'retryStatusCodes' should not contain nulls in $description"
        assert ext.retryExceptions. every { it }, "'retryExceptions' should not contain nulls in $description"

        ext.rootLinks = ( ext.rootLinks?.grep()?.toSet() ?: [ "http://$ext.baseUrl" ]).collect {
            String rootLink ->
            final isGoodEnough = rootLink && rootLink.with { startsWith( 'http://' ) || startsWith( 'https://' )}
            final noSlash      = (( ! rootLink ) || ext.baseUrl.endsWith( '/' ) || rootLink.startsWith( '/' ))
            isGoodEnough ? rootLink : "http://${ ext.baseUrl }${ noSlash ? '' : '/' }${ rootLink ?: '' }"
        }

        assert ext.rootLinks && ext.rootLinks.every{ it }
    }


    @Override
    void taskAction ()
    {
        this.threadPool   = Executors.newFixedThreadPool( ext.threadPoolSize ) as ThreadPoolExecutor
        this.linksStorage = new LinksStorage( ext )

        if ( ext.log ){ delete( ext.log )}

        printStartBanner()
        submitRootLinks()
        waitForIdle()

        printFinishReport()
        writeLinksMapFiles()
        archiveLogFiles()
        checkIfBuildShouldFail()
    }


    @Requires({ ch && input && alternative })
    @Ensures({ result })
    String removeAllAfter( String ch, String input, String alternative )
    {
        final j = input.indexOf( ch )
        ( j > 0 ? new String( input.substring( 0, j )) : alternative )
    }


    /**
     * Logs message returned by the closure provided.
     *
     * @param logLevel           message log level
     * @param error              error thrown
     * @param logMessageCallback closure returning message text
     */
    @Requires({ logLevel && logMessageCallback })
    void crawlerLog ( LogLevel logLevel = LogLevel.INFO, Throwable error = null, Closure logMessageCallback )
    {
        String logText = log( logLevel, error, logMessageCallback )

        if ( ext.log )
        {
            logText = logText ?: logMessageCallback()
            assert logText

            ext.log.append( logText + '\n' )

            if ( error )
            {
                final os = new ByteArrayOutputStream()
                error.printStackTrace( new PrintWriter( os, true ))
                ext.log.append( os.toString() + '\n' )
            }
        }
    }


    /**
     * Prints startup banner.
     */
    void printStartBanner ()
    {
        crawlerLog {
            final ipAddress     = (( ext.rootUrl ==~ /^\d+\.\d+\.\d+\.\d+$/ ) ? '' : " (${ InetAddress.getByName( ext.rootUrl.replaceFirst( /:\d+$/, '' )).hostAddress })" )
            final bannerMessage = "Checking [$ext.baseUrl]${ ipAddress } links with [${ ext.threadPoolSize }] thread${ s( ext.threadPoolSize ) }"
            final bannerLine    = "-" * ( bannerMessage.size() + 2 )
            final os            = new ByteArrayOutputStream()
            final writer        = new PrintWriter( os, true )

            writer.println( bannerLine )
            writer.println( " $bannerMessage" )
            writer.println( " Root link${ s( ext.rootLinks )}:" )
            ext.rootLinks.sort().each { writer.println( " * [$it]" )}
            writer.println( bannerLine )

            os.toString()
        }
    }


    /**
     * Submits root links for checking and starts the crawling process.
     *
     * @param ext
     */
    void submitRootLinks ()
    {
        for ( link in linksStorage.addLinksToProcess( '', ext.rootLinks ).sort())
        {
            final String pageUrl = ( ext.linkTransformers ?: [] ).inject( link ){ String l, Closure c -> c( l )}
            futures << threadPool.submit({ checkLinks( pageUrl, 'Root link', true, 0 )} as Runnable )
        }
    }


    /**
     * Blocks until there is no more activity in a thread pool, meaning all links are checked.
     */
    void waitForIdle ()
    {
        while (( ! crawlingAborted ) && futures.any{ ! it.done } )
        {
            sleep( ext.futuresPollingPeriod )
            futures.removeAll { it.done }

            if ( ext.teamcityMessages )
            {
                final processed = linksProcessed.get()
                final queued    = threadPool.queue.size()
                final broken    = linksStorage.brokenLinksNumber()
                logTeamCityProgressMessage( "$processed link${ s( processed ) } processed, $broken broken, $queued queued" )
            }
        }

        threadPool.queue.clear()
        futures.clear()
        linksStorage.lock()
        threadPool.shutdown()
        threadPool.awaitTermination( 30L, TimeUnit.SECONDS )
    }


    @Requires({ message })
    void logTeamCityProgressMessage ( String message )
    {
        log( LogLevel.WARN ){ "##teamcity[progressMessage '${ message.replace( "'", "|'" ) }']" }
    }


    @Requires({ status && message })
    void logTeamCityBuildStatusMessage ( String status, String message )
    {
        log( LogLevel.WARN ){ "##teamcity[buildStatus status='${ status.replace( "'", "|'" ) }' text='${ message.replace( "'", "|'" ) }']" }
    }


    /**
     * Prints finish report after all links are checked.
     */
    void printFinishReport ()
    {
        if ( ext.teamcityMessages ) { logTeamCityProgressMessage( 'Writing report' )}

        final processedLinks = linksProcessed.get()
        final brokenLinks    = linksStorage.brokenLinksNumber()
        final isSuccess      = ( ! brokenLinks ) && ( ! crawlingAborted )
        final mbDownloaded   = ( long )( bytesDownloaded.get() / ( 1024 * 1024 ))
        final kbDownloaded   = ( long )( bytesDownloaded.get() / ( 1024 ))
        final downloaded     = "[${ mbDownloaded ?: kbDownloaded }] ${ mbDownloaded ? 'Mb' : 'Kb' } downloaded"
        final logLevel       = ( brokenLinks ? LogLevel.ERROR : ext.displaySummary ? LogLevel.WARN : LogLevel.INFO )


        crawlerLog( logLevel ){ "\n\n[$processedLinks] link${ s( processedLinks ) } processed in " +
                                "${( long )(( System.currentTimeMillis() - startTime ) / 1000 )} sec, $downloaded" +
                                ( ext.displayLinks ? ':' : '' )}

        if ( ext.displayLinks )
        {
            final processedLinksLines = toMultiLines( linksStorage.processedLinks())
            crawlerLog( logLevel ){ processedLinksLines }
        }

        crawlerLog( logLevel ){ "\n[$brokenLinks] broken link${ s( brokenLinks ) } found${ brokenLinks ? '\n' : isSuccess ? ' - thumbs up!' : '' }" }

        if ( brokenLinks )
        {
            final joinLines         = { Collection c, String delim = '' -> '\n\n[' + c.join( "]\n$delim[" ) + ']\n\n' }
            final brokenLinksSorted = linksStorage.brokenLinks().sort()

            crawlerLog( logLevel ){ "List of broken links (simple report):\n" }
            for ( brokenLink in brokenLinksSorted )
            {
                crawlerLog( logLevel ){ "- [$brokenLink]" }
            }

            crawlerLog( logLevel ){ "\n\nList of broken links (detailed report):\n" }
            for ( brokenLink in brokenLinksSorted )
            {
                final referrers   = linksStorage.brokenLinkReferrers( brokenLink )
                final linkMessage =
                    "[$brokenLink]\n\n" +
                    ( ext.displayLinksPath ? "Path:${ joinLines( linksStorage.linkPath( brokenLink ), '=>\n' )}" : '' ) +
                    ( "Referred by [${ referrers.size()}] resource${ s( referrers ) }:${ joinLines( referrers ) }" )

                crawlerLog( logLevel ){ "- ${ linkMessage.readLines().join( '\n  ' )}\n" }
            }
        }

        if ( ext.teamcityMessages )
        {
            final status  = (( crawlingAborted || ( ext.failOnBrokenLinks && brokenLinks )) ? 'FAILURE' : 'SUCCESS' )
            final message = "$processedLinks link${ s( processedLinks )}, $brokenLinks broken${ crawlingAborted ?  ', crawling aborted' : '' }"

            logTeamCityBuildStatusMessage( status, message )
        }
    }


    /**
     * Writes "links map" files.
     */
    void writeLinksMapFiles ()
    {
        final print = {
            File file, Map<String, List<String>> linksMap, String title ->

            assert file && ( linksMap != null ) && title

            final linksMapReport = linksMap.keySet().
                                   collect { String pageUrl -> "[$pageUrl]:\n${ toMultiLines( linksMap[ pageUrl ] ) }" }.
                                   join( '\n' )

            write( file, linksMapReport )

            crawlerLog {
                "$title is written to [${ file.canonicalPath }], [${ linksMap.size() }] entr${ s( linksMap.size(), 'y', 'ies' )}"
            }
        }

        if ( ext.linksMapFile    ) { print( ext.linksMapFile,    linksStorage.linksMap(),    'Links map'     )}
        if ( ext.newLinksMapFile ) { print( ext.newLinksMapFile, linksStorage.newLinksMap(), 'New links map' )}
    }


    /**
     * Archives log files if needed.
     */
    void archiveLogFiles()
    {
        final logFiles = [ ext.log, ext.linksMapFile, ext.newLinksMapFile ].grep()

        if ( ext.zipLogFiles && logFiles )
        {
            logFiles.each { zip( it ); delete( it )}
        }
    }


    /**
     * Checks if build should fail and fails it if required.
     */
    void checkIfBuildShouldFail()
    {
        final brokenLinks = linksStorage.brokenLinksNumber()

        if ( crawlingAborted )
        {
            throw new GradleException(
                'Crawling process was aborted, see above for more details' )
        }

        if ( ext.failOnBrokenLinks && brokenLinks )
        {
            throw new GradleException(
                "[$brokenLinks] broken link${ s( brokenLinks )} found, see above for more details" )
        }

        if ( linksProcessed.get() < ext.minimumLinks )
        {
            throw new GradleException(
                "Only [$linksProcessed] link${ s( linksProcessed.get())} checked, " +
                "[${ ext.minimumLinks }] link${ s( ext.minimumLinks )} at least required." )
        }

        if ( bytesDownloaded.get() < ext.minimumBytes )
        {
            throw new GradleException(
                "Only [$bytesDownloaded] byte${ s( bytesDownloaded.get())} downloaded, " +
                "[${ ext.minimumBytes }] byte${ s( ext.minimumBytes )} at least required." )
        }
    }


    /**
     * <b>Invoked in a thread pool worker</b> - checks links in the page specified.
     *
     * @param pageUrl     URL of a page to check its links
     * @param referrerUrl URL of another page referring to the one being checked
     * @param isRootLink  whether url submitted is a root link
     * @param pageDepth   current page depth
     */
    @SuppressWarnings([ 'GroovyMultipleReturnPointsPerMethod' ])
    @Requires({ pageUrl && referrerUrl && ( pageDepth > -1 ) && linksStorage && threadPool })
    void checkLinks ( String pageUrl, String referrerUrl, boolean isRootLink, int pageDepth )
    {
        if ( crawlingAborted ) { return }

        assert (( ext.maxDepth < 0 ) || ( pageDepth <= ext.maxDepth ))
        delay  ( ext.requestDelay )

        try
        {
            final response = readResponse( pageUrl, referrerUrl, isRootLink )

            if ( response.isRedirect )
            {
                final  actualUrlList = filterTransformLinks([ response.actualUrl ]) // List of one element, transformed link
                assert actualUrlList.size().with {( delegate == 0 ) || ( delegate == 1 )}
                if ( actualUrlList && linksStorage.addLinksToProcess( pageUrl, actualUrlList ))
                {
                    checkLinks( actualUrlList.first(), referrerUrl, isRootLink, pageDepth )
                }

                return
            }

            assert pageUrl == response.actualUrl
            final processed = linksProcessed.incrementAndGet()

            if ( ! response.content ){ return }

            final pageContent        = response.asString()
            final pageIgnored        = ( ext.ignoredContent ?: [] ).any { it ( pageUrl, pageContent )}
            final verificationPassed = ( ext.verifyContent  ? verificationPassed( pageUrl, pageContent, ext.verifyContent ) : true )

            if ( ! verificationPassed )
            {
                abortCrawling(  "! Verification of [$pageUrl] has failed" )
                return
            }

            if ( pageIgnored ){ return }
            if ( pageDepth == ext.maxDepth ){ return }

            final List<String> pageLinks = readLinks( pageUrl, pageContent )
            final List<String> newLinks  = ( pageLinks ? linksStorage.addLinksToProcess( pageUrl, pageLinks ) : [] )
            final queued                 = threadPool.queue.size()

            linksStorage.updateBrokenLinkReferrers( pageUrl, pageLinks )

            if ( ext.linksMapFile    && pageLinks ) { linksStorage.updateLinksMap   ( pageUrl, pageLinks )}
            if ( ext.newLinksMapFile && newLinks  ) { linksStorage.updateNewLinksMap( pageUrl, newLinks  )}

            crawlerLog {
                final linksMessage    = pageLinks ? ", ${ newLinks.size() } new"     : ''
                final newLinksMessage = newLinks  ? ": ${ toMultiLines( newLinks )}" : ''

                "[$pageUrl] - depth [$pageDepth], ${ pageLinks.size() } link${ s( pageLinks ) } found$linksMessage, " +
                "$processed processed, $queued queued$newLinksMessage"
            }

            for ( link in newLinks )
            {
                final String linkUrl = link // Otherwise, various invocations share the same "link" instance when invoked
                futures << threadPool.submit({ checkLinks( linkUrl, pageUrl, false, pageDepth + 1 )} as Runnable )
            }
        }
        catch( Throwable error )
        {
            final message = "Unexpected error while reading [$pageUrl], referrer [$referrerUrl]"
            if ( ext.failOnFailure ) { abortCrawling( message, error ) }
            else                     { crawlerLog( LogLevel.ERROR, error ){ message }}
        }
    }


    @Requires({ errorMessage })
    void abortCrawling ( String errorMessage, Throwable error = null )
    {
        crawlingAborted = true
        log( LogLevel.ERROR, error ){ "! $errorMessage, aborting the crawling process" }
        threadPool.shutdownNow()
    }


    /**
     * Invoke verifiers for the page specified and determines if any of them return {@code false} or fails.
     *
     * @param pageUrl     url of the page being checked
     * @param pageContent content of the page being checked
     * @param verifiers   verifiers to invoke
     * @return true if all verifiers returned true when invoked, false otherwise
     */
    @Requires({ pageUrl && pageContent && verifiers })
    boolean verificationPassed( String pageUrl, String pageContent, List<Closure> verifiers )
    {
        try
        {
            verifiers.every { final Object result = it( pageUrl, pageContent ); (( result == null ) || ( result )) }
        }
        catch ( Throwable error )
        {
            log( LogLevel.ERROR, error ){ "Error thrown while verifying [$pageUrl]" }
            false
        }
    }


    /**
     * Reads all hyperlinks in the content specified.
     *
     * @param pageContent content of the page downloaded previously
     * @return all links found in the page content
     */
    @Requires({ pageUrl && pageContent })
    @Ensures({ result != null })
    List<String> readLinks ( String pageUrl, String pageContent )
    {
        String cleanContent = (( String )( ext.pageTransformers ?: [] ).inject( pageContent ){
            String content, Closure transformer -> transformer( pageUrl, content )
        }).replace( '\\', '/' )

        if ( ext.replaceSpecialCharacters )
        {
            cleanContent = cleanContent.replace( '%3A',       ':' ).
                                        replace( '%2F',       '/' ).
                                        replace( '&lt;',      '<' ).
                                        replace( '&gt;',      '>' ).
                                        replace( '&quot;',    '"' ).
                                        replace( '&amp;amp;', '&' ).
                                        replace( '&amp;',     '&' )
        }

        if ( ext.removeHtmlComments )
        {
            cleanContent = cleanContent.replaceAll( htmlCommentPattern, '' )
        }

        final List<String> links = findAll( cleanContent, ext.internalLinkPattern, 2 )

        if ( ext.checkExternalLinks ) {
            final  externalLinks = findAll ( cleanContent, externalLinkPattern, 2 )
            assert externalLinks.every { it.startsWith( 'http://' ) || it.startsWith( 'https://' ) }

            links.addAll( externalLinks )
        }

        if ( ext.checkAbsoluteLinks ) {
            final  absoluteLinks = findAll ( cleanContent, absoluteLinkPattern, 2 )
            assert absoluteLinks.every{ it.startsWith( '/' ) }

            links.addAll( absoluteLinks.collect{( it.startsWith( '//' ) ? "http://${ it.replaceAll( slashesPattern, '' )}" :
                                                                          "http://$ext.rootUrl$it" ).toString() })
        }

        if ( ext.checkRelativeLinks ) {

            final pageBaseUrl    = pageUrl.replaceFirst( relativeLinkReminderPattern, '' )
            final requestBaseUrl = removeAllAfter( '?', pageUrl, pageBaseUrl )
            final relativeLinks  = findAll ( cleanContent, relativeLinkPattern, 2 )
            assert ( ! pageBaseUrl.endsWith( '/' )) && ( ! requestBaseUrl.endsWith( '?' )) && relativeLinks.every { ! it.startsWith( '/' )}

            links.addAll( relativeLinks.collect{( it.startsWith( '?' ) ? "$requestBaseUrl$it" : "$pageBaseUrl/$it" ).toString() })
        }

        assert links.every{ it }
        filterTransformLinks( links )
    }


    @Requires({ links != null })
    @Ensures({ result != null })
    List<String> filterTransformLinks ( Collection<String> links )
    {
        ( List<String> ) links.collect { String link -> normalizeUrl( removeAllAfter( '#', link, link )) }.
                               toSet().
                               findAll { String link -> ( ! ( ext.ignoredLinks ?: [] ).any { it( link ) }) }.
                               collect { String link -> ( ext.linkTransformers ?: [] ).inject( link ){ String l, Closure c -> c( l ) }}
    }


    @Requires({ pageUrl })
    @Ensures({ result })
    String normalizeUrl( String pageUrl )
    {
        try              { return pageUrl.toURI().normalize().toURL().toString() }
        catch ( ignored ){ return pageUrl }
    }


    /**
     * Retrieves {@code byte[]} content of the link specified.
     *
     * @param pageUrl         URL of a link to read
     * @param referrerUrl     URL of link referrer
     * @param forceGetRequest whether a link should be GET-requested regardless of its type
     * @param attempt         Number of the current attempt, starts from 1
     *
     * @return response data container
     */
    @Requires({ pageUrl && referrerUrl && linksStorage && ( attempt > 0 ) })
    @Ensures({ result })
    CrawlerHttpResponse readResponse ( final String  pageUrl,
                                       final String  referrerUrl,
                                       final boolean forceGetRequest,
                                       final int     attempt = 1 )
    {
        final        htmlLink          = ( ! pageUrl.toLowerCase().with{ ( ext.nonHtmlExtensions - ext.htmlExtensions ).any{ endsWith( ".$it" ) || contains( ".$it?" ) }} ) &&
                                         ( ! ( ext.nonHtmlLinks ?: [] ).any{ it( pageUrl ) })
        final        readFullContent   = ( htmlLink && pageUrl.with { startsWith( "http://${ ext.baseUrl  }" ) ||
                                                                      startsWith( "https://${ ext.baseUrl }" ) })
        final        isHeadRequest     = (( ! forceGetRequest ) && ( ! readFullContent ))
        final        requestMethod     = ( isHeadRequest ? 'HEAD' : 'GET' )
        final        linksStorageLocal = linksStorage // So that the closure that follows can access it
        final        crawlerResponse   = { HttpResponse r -> new CrawlerHttpResponse( r, referrerUrl, linksStorageLocal, attempt )}
        CrawlerHttpResponse response   = crawlerResponse( new HttpResponse( pageUrl, requestMethod ))

        try
        {
            final t  = System.currentTimeMillis()
            crawlerLog{ "[$pageUrl] - sending $requestMethod request .." }
            response = crawlerResponse ( httpRequest( pageUrl,
                                                      requestMethod,
                                                      [ 'User-Agent' : ext.userAgent, 'Connection': 'keep-alive' ],
                                                      ext.connectTimeout,
                                                      ext.readTimeout,
                                                      true, true, null, null,
                                                      { HttpResponse r -> ( readFullContent && ( ! r.isRedirect ))}))

            if (( response.data != null ) && ( response.content != null ))
            {   // Response was read, but it can be empty
                final responseSize         = response.data.length
                final contentSize          = response.content.length
                final totalBytesDownloaded = bytesDownloaded.addAndGet( responseSize )

                crawlerLog {
                    "[$pageUrl] - [$responseSize${ ( responseSize != contentSize ) ? ' => ' + contentSize : '' }] " +
                    "byte${ s( Math.max( responseSize, contentSize )) }, [${ System.currentTimeMillis() - t }] ms"
                }

                checkDownloadLimits( pageUrl, responseSize, totalBytesDownloaded )
            }
            else
            {   // Response wasn't read
                assert response.inputStream
                bytesDownloaded.addAndGet(( isHeadRequest || response.isRedirect || ( response.inputStream.read() == -1 )) ? 0 : 1 )
                response.inputStream.close()

                crawlerLog {
                    "[$pageUrl] - " +
                    ( response.isRedirect ? "redirected to [$response.actualUrl], " : 'can be read, ' ) +
                    "[${ System.currentTimeMillis() - t }] ms"
                }
            }

            response
        }
        catch ( Throwable error )
        {
            handleError( response, error )
        }
    }


    @Requires({ pageUrl && ( responseSize >= 0 ) && ( totalBytesDownloaded >= 0 ) })
    void checkDownloadLimits( String pageUrl, long responseSize, long totalBytesDownloaded )
    {
        if (( ext.pageDownloadLimit > 0 ) && ( responseSize > ext.pageDownloadLimit ))
        {
            abortCrawling( "[$pageUrl] - response size of [$responseSize] byte${ s( responseSize ) } " +
                           "exceeds the per page download limit of [$ext.pageDownloadLimit] byte${ s( ext.pageDownloadLimit ) }" )
            return
        }

        if (( ext.totalDownloadLimit > 0 ) && ( totalBytesDownloaded > ext.totalDownloadLimit ))
        {
            abortCrawling( "Total amount of bytes download [$totalBytesDownloaded] " +
                           "exceeds the total download limit of [$ext.totalDownloadLimit] byte${ s( ext.totalDownloadLimit ) }" )
        }
    }


    /**
     * Handles the error thrown while reading the response.
     *
     * @param response response data container
     * @param error error thrown
     * @return new response data (if request was retried) or the same instance that was specified
     */
    @Requires({ response && error })
    @Ensures({ result })
    CrawlerHttpResponse handleError ( CrawlerHttpResponse response, Throwable error )
    {
        response.with {
            final isRetryMatch = ( ext.retryStatusCodes?.any { it == statusCode } ||
                                   ext.retryExceptions?. any { it.isInstance( error ) || it.isInstance( statusCode ) })
            final isRetry      = ( isHeadRequest || ( isRetryMatch && ( attempt < ext.retries )))
            final isAttempt    = (( ! isHeadRequest ) && ( ext.retries > 1 ) && ( isRetryMatch ))
            final errorMessage = "! [$actualUrl] - $error, status code [${ ( statusCode instanceof Integer ) ? statusCode : 'unknown' }]"

            if ( isRetry )
            {
                assert ( isHeadRequest || isAttempt )
                crawlerLog { "$errorMessage, ${ isHeadRequest ? 'will be retried as GET request' : 'attempt ' + attempt }" }

                delay( ext.retryDelay )
                readResponse( actualUrl, referrerUrl, true, isHeadRequest ? 1 : attempt + 1 )
            }
            else
            {
                errorMessage = "$errorMessage${ isAttempt ? ', attempt ' + attempt : '' }"

                if (( ext.ignoredBrokenLinks ?: [] ).any{ it ( actualUrl )})
                {
                    crawlerLog{ "$errorMessage, not registered as broken link - filtered out by ignoredBrokenLinks" }
                }
                else
                {
                    crawlerLog{ "$errorMessage, registered as broken link" }
                    linksStorage.addBrokenLink( originalUrl, referrerUrl )
                }

                response
            }
        }
    }


    /**
     * Converts collection specified to multi-line String.
     * @param c Collection to convert.
     * @param delimiter Delimiter to use on every line.
     *
     * @return collection specified converted to multi-line String
     */
    @Requires({ c != null })
    @Ensures({ result })
    String toMultiLines( Collection c, String delimiter = '*' ){ "\n$delimiter [${ c.sort().join( "]\n$delimiter [" ) }]\n" }
}
