package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BaseTask

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream


/**
 * {@link CrawlerPlugin} task.
 */
class CrawlerTask extends BaseTask<CrawlerExtension>
{
    private final Queue<Future> futures         = new ConcurrentLinkedQueue<Future>()
    private final AtomicLong    bytesDownloaded = new AtomicLong( 0L )
    private final AtomicLong    linksProcessed  = new AtomicLong( 0L )

    private volatile boolean            verificationFlag = true // If ever becomes false - crawling process is aborted immediately
    private          ThreadPoolExecutor threadPool
    private          LinksStorage       linksStorage


    /**
     * Verifies {@link CrawlerExtension} contains proper settings and updates it with additional properties.
     * @return {@link CrawlerExtension} instance verified and updated.
     */
    @Override
    void verifyExtension( String description )
    {
        assert ext.externalLinkPattern         &&
               ext.absoluteLinkPattern         &&
               ext.relativeLinkPattern         &&
               ext.relativeLinkReminderPattern &&
               ext.htmlCommentPattern          &&
               ext.slashesPattern

        assert ( ! ext.rootUrl             ), "'rootUrl' should not be used in $description - private area"
        assert ( ! ext.internalLinkPattern ), "'internalLinkPattern' should not be used in $description - private area"

        ext.baseUrl             = ext.baseUrl?.trim()?.replace( '\\', '/' )?.replaceAll( '^.+?:/+', '' ) // Protocol part removed
        ext.rootUrl             = ext.baseUrl?.replaceAll( '/.*', '' )                                   // Path part removed
        ext.internalLinkPattern = Pattern.compile( /(?:'|"|>)(https?:\/\/\Q${ ext.baseUrl }\E.*?)(?:'|"|<)/ )

        assert ext.baseUrl, "'baseUrl' should be defined in $description"
        assert ext.rootUrl && ( ! ext.rootUrl.endsWith( '/' )) && ext.internalLinkPattern

        assert ext.userAgent,                 "'userAgent' should be defined in $description"
        assert ext.threadPoolSize       >  0, "'threadPoolSize' [${ ext.threadPoolSize }] in $description should be positive"
        assert ext.connectTimeout       >  0, "'connectTimeout' [${ ext.connectTimeout }] in $description should be positive"
        assert ext.readTimeout          >  0, "'readTimeout' [${ ext.readTimeout }] in $description should be positive"
        assert ext.checksumsChunkSize   >  0, "'checksumsChunkSize' [${ ext.checksumsChunkSize }] in $description should be positive"
        assert ext.futuresPollingPeriod >  0, "'futuresPollingPeriod' [${ ext.futuresPollingPeriod }] in $description should be positive"
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


    /**
     * Passes a new extensions object to the closure specified.
     * Registers new extension under task's name.
     */
    @Requires({ c })
    void config( Closure c )
    {
        this.extensionName = this.name
        this.ext           = project.extensions.create( this.extensionName, CrawlerExtension )
        c( this.ext )
    }


    @Override
    void taskAction ()
    {
        this.threadPool   = Executors.newFixedThreadPool( ext.threadPoolSize ) as ThreadPoolExecutor
        this.linksStorage = new LinksStorage( ext )

        assert (( ! ext.log ) || ( ! ext.log.file ) || project.delete( ext.log )), \
               "Failed to delete [${ ext.log.canonicalPath }]"

        printStartBanner()
        submitRootLinks()
        waitForIdleOrVerificationFailure()

        printFinishReport()
        writeLinksMapFiles()
        archiveLogFiles()
        checkIfBuildShouldFail()
    }


    @Requires({ delayInMilliseconds > -1 })
    void delay( long delayInMilliseconds )
    {
        if ( delayInMilliseconds > 0 ){ sleep( delayInMilliseconds )}
    }


    @Requires({ ch && input && alternative })
    @Ensures({ result })
    String removeAllAfter( String ch, String input, String alternative )
    {
        final j = input.indexOf( ch )
        ( j > 0 ? input.substring( 0, j ) : alternative )
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
            final ipAddress     = (( ext.rootUrl ==~ /^\d+\.\d+\.\d+\.\d+$/ ) ? '' : " (${ InetAddress.getByName( ext.rootUrl ).hostAddress })" )
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
        for ( link in linksStorage.addLinksToProcess( ext.rootLinks ).sort())
        {
            final String pageUrl = ( ext.linkTransformers ?: [] ).inject( link ){ String l, Closure c -> c( l )}
            futures << threadPool.submit({ checkLinks( pageUrl, 'Root link', 'Root link', true, 0 )} as Runnable )
        }
    }


    /**
     * Blocks until there is no more activity in a thread pool, meaning all links are checked.
     */
    void waitForIdleOrVerificationFailure ()
    {
        while ( verificationFlag && futures.any{ ! it.done } )
        {
            sleep( ext.futuresPollingPeriod )
            futures.removeAll { it.done }

            if ( ext.teamcityMessages )
            {
                final processed = linksProcessed.get()
                final queued    = threadPool.queue.size()
                logger.warn( "##teamcity[progressMessage '$processed link${ s( processed ) } processed, $queued queued']" )
            }
        }

        threadPool.queue.clear()
        futures.clear()
        linksStorage.lock()
        threadPool.shutdown()
        threadPool.awaitTermination( 30L, TimeUnit.SECONDS )
    }


    /**
     * Prints finish report after all links are checked.
     */
    void printFinishReport ()
    {
        final processedLinks = linksProcessed.get()
        final brokenLinks    = linksStorage.brokenLinksNumber()
        final mbDownloaded   = ( long )( bytesDownloaded.get() / ( 1024 * 1024 ))
        final kbDownloaded   = ( long )( bytesDownloaded.get() / ( 1024 ))
        final downloaded     = "[${ mbDownloaded ?: kbDownloaded }] ${ mbDownloaded ? 'Mb' : 'Kb' } downloaded"
        final message        = new StringBuilder().
                               append( "\n\n[$processedLinks] link${ s( processedLinks ) } processed in ".toString()).
                               append( "${( long )(( System.currentTimeMillis() - startTime ) / 1000 )} sec, ".toString()).
                               append( downloaded.toString())

        if ( ext.displayLinks )
        {
            message.append( ':\n' ).
                    append( toMultiLines( linksStorage.processedLinks()))
        }

        message.append( "\n[$brokenLinks] broken link${ s( brokenLinks ) } found".toString())

        if ( brokenLinks )
        {
            message.append( ':\n\n' )
            for ( brokenLink in linksStorage.brokenLinks().sort())
            {
                message.append( "- [$brokenLink] - referred to by".toString()).
                        append( toMultiLines( linksStorage.brokenLinkReferrers( brokenLink ), ' ' )).
                        append( '\n' )
            }
        }
        else if ( verificationFlag )
        {
            message.append( ' - thumbs up!' )
        }

        final logLevel = ( brokenLinks ? LogLevel.ERROR : ext.printSummary ? LogLevel.WARN : LogLevel.INFO )
        crawlerLog( logLevel ){ message.toString() }

        if ( ext.teamcityMessages )
        {
            final status = ((( ! verificationFlag ) || ( ext.failOnBrokenLinks && brokenLinks )) ? 'FAILURE' : 'SUCCESS' )
            final text   = "$processedLinks link${ s( processedLinks )}, $brokenLinks broken${ verificationFlag ? '' : ', verification failure' }"
            log( LogLevel.WARN ){ "##teamcity[buildStatus status='$status' text='$text']" }
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

            file.write( linksMapReport, 'UTF-8' )
            if ( linksMap ){ assert file.size()}

            crawlerLog {
                "$title is written to [${ file.canonicalPath }], [${ linksMap.size() }] entr${ linksMap.size() == 1 ? 'y' : 'ies' }"
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
            logFiles.each { zip( it ); project.delete( it )}
        }
    }


    /**
     * Checks if build should fail and fails it if required.
     */
    void checkIfBuildShouldFail()
    {
        final brokenLinks = linksStorage.brokenLinksNumber()

        if ( ! verificationFlag )
        {
            throw new GradleException(
                'Crawling process was aborted due to verification failure, see above for more details' )
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
     * @param pageUrl         URL of a page to check its links
     * @param referrerUrl     URL of another page referring to the one being checked
     * @param referrerContent Referrer page content
     * @param isRootLink      whether url submitted is a root link
     * @param pageDepth       current page depth
     */
    @SuppressWarnings([ 'GroovyMultipleReturnPointsPerMethod' ])
    @Requires({ pageUrl && referrerUrl && referrerContent && ( pageDepth > -1 ) && linksStorage && threadPool })
    void checkLinks ( String pageUrl, String referrerUrl, String referrerContent, boolean isRootLink, int pageDepth )
    {
        if ( ! verificationFlag ) { return }

        assert (( ext.maxDepth < 0 ) || ( pageDepth <= ext.maxDepth ))
        delay  ( ext.requestDelay )

        try
        {
            final response   = readResponse( pageUrl, referrerUrl, referrerContent, isRootLink )
            String actualUrl = response.actualUrl

            if ( pageUrl != actualUrl )
            {
                final  actualUrlList = filterTransformLinks([ actualUrl ]) // List of one element, transformed link
                assert actualUrlList.size().with {( delegate == 0 ) || ( delegate == 1 )}
                if ( actualUrlList && linksStorage.addLinksToProcess( actualUrlList ))
                {
                    checkLinks( actualUrlList.first(), referrerUrl, referrerContent, isRootLink, pageDepth )
                }

                return
            }

            assert pageUrl == actualUrl
            final processed = linksProcessed.incrementAndGet()

            if ( ! response.data ){ return }

            final pageContent        = new String( response.data, 'UTF-8' )
            final pageIgnored        = ( ext.ignoredContent ?: [] ).any { it ( pageUrl, pageContent )}
            final verificationPassed = ( ext.verifyContent  ? verificationPassed( pageUrl, pageContent, ext.verifyContent ) : true )

            if ( ! verificationPassed )
            {
                verificationFlag = false

                log( LogLevel.ERROR ){ "! Verification of [$pageUrl] has failed, aborting the crawling process" }
                threadPool.shutdownNow()
                return
            }

            if ( pageIgnored ){ return }
            if ( pageDepth == ext.maxDepth ){ return }

            final List<String> pageLinks = readLinks( pageUrl, pageContent )
            final List<String> newLinks  = ( pageLinks ? linksStorage.addLinksToProcess( pageLinks ) : [] )
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
                futures << threadPool.submit({ checkLinks( linkUrl, pageUrl, pageContent, false, pageDepth + 1 )} as Runnable )
            }
        }
        catch( Throwable error )
        {
            crawlerLog( LogLevel.ERROR, error ){ "Unexpected error while reading [$pageUrl], referrer [$referrerUrl]" }
        }
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
            cleanContent = cleanContent.replaceAll( ext.htmlCommentPattern, '' )
        }

        final List<String> links = findAll( cleanContent, ext.internalLinkPattern )

        if ( ext.checkExternalLinks ) {
            final  externalLinks = findAll ( cleanContent, ext.externalLinkPattern )
            assert externalLinks.every { it.startsWith( 'http://' ) || it.startsWith( 'https://' ) }

            links.addAll( externalLinks )
        }

        if ( ext.checkAbsoluteLinks ) {
            final  absoluteLinks = findAll ( cleanContent, ext.absoluteLinkPattern )
            assert absoluteLinks.every{ it.startsWith( '/' ) }

            links.addAll( absoluteLinks.collect{( it.startsWith( '//' ) ? "http://${ it.replaceAll( ext.slashesPattern, '' )}" :
                                                                          "http://$ext.rootUrl$it" ).toString() })
        }

        if ( ext.checkRelativeLinks ) {

            final pageBaseUrl    = pageUrl.replaceFirst( ext.relativeLinkReminderPattern, '' )
            final requestBaseUrl = removeAllAfter( '?', pageUrl, pageBaseUrl )
            final relativeLinks  = findAll ( cleanContent, ext.relativeLinkPattern )
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
        links.collect { normalizeUrl( removeAllAfter( '#', it, it )) }.
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
     * @param referrer        URL of link referrer
     * @param referrerContent Referrer page content
     * @param forceGetRequest whether a link should be GET-requested regardless of its type
     * @param attempt         Number of the current attempt, starts from 1
     *
     * @return response data container
     */
    @Requires({ pageUrl && referrer && referrerContent && linksStorage && ( attempt > 0 ) })
    @Ensures({ result.actualUrl })
    ResponseData readResponse ( final String  pageUrl,
                                final String  referrer,
                                final String  referrerContent,
                                final boolean forceGetRequest,
                                final int     attempt = 1 )
    {
        InputStream  inputStream     = null
        final        htmlLink        = ( ! pageUrl.toLowerCase().with{ ( ext.nonHtmlExtensions - ext.htmlExtensions ).any{ endsWith( ".$it" ) || contains( ".$it?" ) }} ) &&
                                       ( ! ( ext.nonHtmlLinks ?: [] ).any{ it( pageUrl ) })
        boolean      readFullContent = ( htmlLink && pageUrl.with { startsWith( "http://${ ext.baseUrl  }" ) ||
                                                                    startsWith( "https://${ ext.baseUrl }" ) })
        final        isHeadRequest   = (( ! forceGetRequest ) && ( ! readFullContent ))
        final        requestMethod   = ( isHeadRequest ? 'HEAD' : 'GET' )
        final        response        = new ResponseData( pageUrl, referrer, referrerContent, linksStorage, attempt, forceGetRequest, isHeadRequest )

        try
        {
            crawlerLog{ "[$pageUrl] - sending $requestMethod request .." }

            final t             = System.currentTimeMillis()
            response.connection = openConnection( pageUrl , requestMethod )
            inputStream         = response.connection.inputStream
            response.actualUrl  = response.connection.getURL().toString()
            final isRedirect    = ( pageUrl != response.actualUrl )

            if ( readFullContent && ( ! isRedirect ))
            {
                response.data      = inputStream.bytes
                final responseSize = response.data.length

                response.data      = decodeResponseData( response )
                final contentSize  = response.data.length

                bytesDownloaded.addAndGet( responseSize )

                crawlerLog {
                    "[$pageUrl] - [$responseSize${ ( responseSize != contentSize ) ? ' => ' + contentSize : '' }] " +
                    "byte${ s( Math.max( responseSize, contentSize )) }, [${ System.currentTimeMillis() - t }] ms"
                }
            }
            else
            {
                response.data = []
                bytesDownloaded.addAndGet(( isRedirect || ( inputStream.read() == -1 )) ? 0 : 1 )

                crawlerLog {
                    "[$pageUrl] - " +
                    ( isRedirect ? "redirected to [$response.actualUrl], " : 'can be read, ' ) +
                    "[${ System.currentTimeMillis() - t }] ms"
                }
            }

            response
        }
        catch ( Throwable error )
        {
            handleError( response, error )
        }
        finally
        {
            if ( inputStream ){ inputStream.close() }
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
    ResponseData handleError ( ResponseData response, Throwable error )
    {
        response.with {
            final statusCode      = ( connection ? statusCode ( connection )       : null )
            final statusCodeError = ( statusCode instanceof Throwable ? statusCode : null )
            final isRetryMatch    = ( ext.retryStatusCodes?.any { it == statusCode } ||
                                      ext.retryExceptions?. any { it.isInstance( error ) || it.isInstance( statusCodeError ) })
            final isRetry         = ( isHeadRequest || ( isRetryMatch && ( attempt < ext.retries )))
            final isAttempt       = (( ! isHeadRequest ) && ( ext.retries > 1 ) && ( isRetryMatch ))
            final logMessage      = "! [$actualUrl] - $error, status code [${ (( statusCode == null ) || statusCodeError ) ? 'unknown' : statusCode }]"

            if ( isRetry )
            {
                assert ( isHeadRequest || isAttempt )
                crawlerLog { "$logMessage, ${ isHeadRequest ? 'will be retried as GET request' : 'attempt ' + attempt }" }

                delay( ext.retryDelay )
                readResponse( actualUrl, referrer, referrerContent, true, isHeadRequest ? 1 : attempt + 1 )
            }
            else
            {
                logMessage = "$logMessage${ isAttempt ? ', attempt ' + attempt : '' }"

                if (( ext.ignoredBrokenLinks ?: [] ).any{ it ( actualUrl )})
                {
                    crawlerLog{ "$logMessage, not registered as broken link - filtered out by ignoredBrokenLinks" }
                }
                else
                {
                    crawlerLog{ "$logMessage, registered as broken link" }
                    linksStorage.addBrokenLink( originalUrl, referrer )
                }

                response
            }
        }
    }


    /**
     * Attempts to read connection status code.
     * @param connection connection to read its status code
     * @return connection status code or exception thrown
     */
    @Requires({ connection })
    @Ensures({ result })
    Object statusCode( HttpURLConnection connection )
    {
        try { connection.responseCode }
        catch ( Throwable error ) { error }
    }


    @Requires({ pageUrl && requestMethod })
    @Ensures({ result })
    HttpURLConnection openConnection ( String pageUrl, String requestMethod )
    {
        /**
         * Full-blown URL encoding: new URL( pageUrl ).with { new URI( protocol, userInfo, host, port, path, query, ref ).toURL()}
         * It doesn't work with URLs that are already encoded and is slow, so we simply replace ' ' to '+'.
         */
        final connection          = pageUrl.replace( ' ' as char, '+' as char ).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = ext.connectTimeout
        connection.readTimeout    = ext.readTimeout
        connection.requestMethod  = requestMethod

        connection.setRequestProperty( 'User-Agent' ,     ext.userAgent  )
        connection.setRequestProperty( 'Accept-Encoding', 'gzip,deflate' )
        connection.setRequestProperty( 'Connection' ,     'keep-alive'   )

        connection
    }


    @Requires({ response?.connection && ( response?.data != null ) })
    @Ensures({ result != null })
    byte[] decodeResponseData ( ResponseData response )
    {
        final contentEncoding = response.connection.getHeaderField( 'Content-Encoding' )

        if ( ! ( contentEncoding && response.data )) { return response.data }

        final contentLength = Integer.valueOf( response.connection.getHeaderField( 'Content-Length' ) ?: '-1' )
        final bufferSize    = ((( contentLength > 0 ) && ( contentLength < ( 100 * 1024 ))) ? contentLength : 10 * 1024 )
        final inputStream   = new ByteArrayInputStream( response.data ).with {
            InputStream is ->
            ( 'gzip'    == contentEncoding ) ? new GZIPInputStream( is, bufferSize ) :
            ( 'deflate' == contentEncoding ) ? new DeflaterInputStream( is, new Deflater(), bufferSize ) :
                                               null
        }

        assert inputStream, "Unknown response content encoding [$contentEncoding]"
        inputStream.bytes
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
