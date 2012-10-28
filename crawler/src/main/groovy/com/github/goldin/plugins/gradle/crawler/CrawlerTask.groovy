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
class CrawlerTask extends BaseTask
{
    private       String             extensionName   = CrawlerPlugin.EXTENSION_NAME
    private final Queue<Future>      futures         = new ConcurrentLinkedQueue<Future>()
    private final AtomicLong         bytesDownloaded = new AtomicLong( 0L )
    private final AtomicLong         linksProcessed  = new AtomicLong( 0L )

    private       ThreadPoolExecutor threadPool
    private       LinksStorage       linksStorage

    CrawlerExtension ext () { extension ( this.extensionName, CrawlerExtension ) }


    /**
     * Passes a new extensions object to the closure specified (to use it later).
     */
    void config( Closure c ){
        this.extensionName = this.name
        final extension    = new CrawlerExtension()
        project.extensions.add( extensionName, extension )
        c( extension )
    }


    @Override
    void taskAction ()
    {
        final ext         = verifyAndUpdateExtension()
        this.threadPool   = Executors.newFixedThreadPool( ext.threadPoolSize ) as ThreadPoolExecutor
        this.linksStorage = new LinksStorage( ext )

        assert (( ! ext.log ) || ( ! ext.log.file ) || project.delete( ext.log )), \
               "Failed to delete [${ ext.log.canonicalPath }]"

        printStartBanner()
        submitRootLinks()
        waitForIdle()
        printReport()
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


    @Requires({ link })
    boolean isInternalLink( String link )
    {
        link.startsWith( "http://${ ext().baseUrl }"  ) || link.startsWith( "https://${ ext().baseUrl }" )
    }


    /**
     * Logs message returned by the closure provided.
     *
     * @param logLevel   message log level
     * @param error      error thrown
     * @param logMessage closure returning message text
     */
    @Requires({ logLevel && logMessage })
    void log( LogLevel logLevel = LogLevel.INFO, Throwable error = null, Closure logMessage )
    {
        final  ext     = ext()
        String logText = null

        if ( logger.isEnabled( logLevel ))
        {
            logText = logMessage()
            assert logText

            if ( error ) { logger.log( logLevel, logText, error )}
            else         { logger.log( logLevel, logText )}
        }

        if ( ext.log )
        {
            logText = logText ?: logMessage()
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
     * Verifies {@link CrawlerExtension} contains proper settings and updates it with additional properties.
     * @return {@link CrawlerExtension} instance verified and updated.
     */
    @Ensures({ result })
    CrawlerExtension verifyAndUpdateExtension ()
    {
        final ext                  = ext()
        final extensionDescription = "${ this.extensionName } { .. }"

        assert ext.externalLinkPattern &&
               ext.absoluteLinkPattern &&
               ext.relativeLinkPattern &&
               ext.relativeLinkReminderPattern &&
               ext.htmlCommentPattern

        assert ( ! ext.rootUrl             ), "'rootUrl' should not be used in $extensionDescription - private area"
        assert ( ! ext.internalLinkPattern ), "'internalLinkPattern' should not be used in $extensionDescription - private area"

        ext.baseUrl             = ext.baseUrl?.trim()?.replace( '\\', '/' )?.replaceAll( '^.+?:/+', '' ) // Protocol part removed
        ext.rootUrl             = ext.baseUrl?.replaceAll( '/.*', '' )                                   // Path part removed
        ext.internalLinkPattern = Pattern.compile( /(?:'|"|>)(https?:\/\/\Q${ ext.baseUrl }\E.*?)(?:'|"|<)/ )

        assert ext.baseUrl, "'baseUrl' should be defined in $extensionDescription"
        assert ext.rootUrl && ( ! ext.rootUrl.endsWith( '/' )) && ext.internalLinkPattern

        assert ext.userAgent,                 "'userAgent' should be defined in $extensionDescription"
        assert ext.threadPoolSize       >  0, "'threadPoolSize' [${ ext.threadPoolSize }] in $extensionDescription should be positive"
        assert ext.connectTimeout       >  0, "'connectTimeout' [${ ext.connectTimeout }] in $extensionDescription should be positive"
        assert ext.readTimeout          >  0, "'readTimeout' [${ ext.readTimeout }] in $extensionDescription should be positive"
        assert ext.checksumsChunkSize   >  0, "'checksumsChunkSize' [${ ext.checksumsChunkSize }] in $extensionDescription should be positive"
        assert ext.futuresPollingPeriod >  0, "'futuresPollingPeriod' [${ ext.futuresPollingPeriod }] in $extensionDescription should be positive"
        assert ext.retries              > -1, "'retries' [${ ext.retries }] in $extensionDescription should not be negative"
        assert ext.retryDelay           > -1, "'retryDelay' [${ ext.retryDelay }] in $extensionDescription should not be negative"
        assert ext.requestDelay         > -1, "'requestDelay' [${ ext.requestDelay }] in $extensionDescription should not be negative"

        assert ext.retryStatusCodes.every { it }, "'retryStatusCodes' should not contain nulls in $extensionDescription"
        assert ext.retryExceptions. every { it }, "'retryExceptions' should not contain nulls in $extensionDescription"

        ext.rootLinks = ( ext.rootLinks?.grep()?.toSet()?.sort() ?: [ "http://$ext.baseUrl" ]).collect {
            String rootLink ->
            final isGoodEnough = rootLink && rootLink.with { startsWith( 'http://' ) || startsWith( 'https://' )}
            final noSlash      = (( ! rootLink ) || ext.baseUrl.endsWith( '/' ) || rootLink.startsWith( '/' ))
            isGoodEnough ? rootLink : "http://${ ext.baseUrl }${ noSlash ? '' : '/' }${ rootLink ?: '' }"
        }
        assert ext.rootLinks && ext.rootLinks.every{ it }
        ext
    }


    /**
     * Prints startup banner.
     */
    void printStartBanner ()
    {
        log {
            final ext  = ext()

            final ipAddress     = (( ext.rootUrl ==~ /^\d+\.\d+\.\d+\.\d+$/ ) ? '' : " (${ InetAddress.getByName( ext.rootUrl ).hostAddress })" )
            final bannerMessage = "Checking [$ext.baseUrl]${ ipAddress } links with [${ ext.threadPoolSize }] thread${ s( ext.threadPoolSize ) }"
            final bannerLine    = "-" * ( bannerMessage.size() + 2 )
            final os            = new ByteArrayOutputStream()
            final writer        = new PrintWriter( os, true )

            writer.println( bannerLine )
            writer.println( " $bannerMessage" )
            writer.println( " Root link${ s( ext.rootLinks )}:" )
            ext.rootLinks.each { writer.println( " * [$it]" )}
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
        final ext = ext()
        for ( link in linksStorage.addLinksToProcess( ext.rootLinks ))
        {
            final pageUrl = link // Otherwise, various invocations share the same "link" instance when invoked
            futures << threadPool.submit({ checkLinks( pageUrl, 'Root link', 'Root link', true )} as Runnable )
        }
    }


    /**
     * Blocks until there is no more activity in a thread pool, meaning all links are checked.
     */
    void waitForIdle ()
    {
        final ext = ext()

        while ( futures.any{ ! it.done })
        {
            sleep( ext.futuresPollingPeriod )
            futures.removeAll { it.done }
        }

        linksStorage.lock()
        threadPool.shutdown()
        threadPool.awaitTermination( 1L, TimeUnit.SECONDS )
    }


    /**
     * Prints final report after all links are checked.
     */
    void printReport ()
    {
        final processedLinks = linksProcessed.get()
        final ext            = ext()
        final mbDownloaded   = ( long )( bytesDownloaded.get() / ( 1024 * 1024 ))
        final kbDownloaded   = ( long )( bytesDownloaded.get() / ( 1024 ))
        final downloaded     = "[${ mbDownloaded ?: kbDownloaded }] ${ mbDownloaded ? 'Mb' : 'Kb' } downloaded"

        final message = new StringBuilder().append( "\n\n[$processedLinks] link${ s( processedLinks ) } processed in ".toString()).
                                            append( "${( long )(( System.currentTimeMillis() - startTime ) / 1000 )} sec, ".toString()).
                                            append( downloaded.toString())

        if ( ext.displayLinks )
        {
            message.append( ':\n' ).
                    append( toMultiLines( linksStorage.processedLinks()))
        }

        message.append( "\n[${ linksStorage.brokenLinksNumber()}] broken link${ s( linksStorage.brokenLinksNumber()) } found".toString()).
                append( linksStorage.brokenLinksNumber() ? '' : ' - thumbs up!' )

        if ( linksStorage.brokenLinksNumber())
        {
            message.append( ':\n\n' )
            for ( brokenLink in linksStorage.brokenLinks().sort())
            {
                message.append( "- [$brokenLink] - referred to by".toString()).
                        append( toMultiLines( linksStorage.brokenLinkReferrers( brokenLink ), ' ' )).
                        append( '\n' )
            }
        }

        final logLevel = ( linksStorage.brokenLinksNumber() ? LogLevel.ERROR :
                           ext.printSummary                 ? LogLevel.WARN  :
                                                              LogLevel.INFO )
        log( logLevel ){ message.toString() }
    }


    /**
     * Writes "links map" files.
     */
    void writeLinksMapFiles ()
    {
        final ext   = ext()
        final print = {
            File file, Map<String, Set<String>> linksMap, String title ->

            assert file && ( linksMap != null ) && title

            final linksMapReport = linksMap.keySet().sort().
                                   collect { String pageUrl -> "[$pageUrl]:\n${ toMultiLines( linksMap[ pageUrl ]) }" }.
                                   join( '\n' )

            file.write( linksMapReport, 'UTF-8' )
            if ( linksMap ){ assert file.size()}

            log {
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
        final ext = ext()

        if ( ext.zipLogFiles && ( ext.log || ext.linksMapFile || ext.newLinksMapFile ))
        {
            zip           ( ext.log, ext.linksMapFile, ext.newLinksMapFile )
            project.delete( ext.log, ext.linksMapFile, ext.newLinksMapFile )
        }
    }


    /**
     * Checks if build should fail and fails it if required.
     */
    void checkIfBuildShouldFail()
    {
        final ext = ext()

        if ( linksStorage.brokenLinksNumber() && ext.failOnBrokenLinks )
        {
            throw new GradleException(
                "[${ linksStorage.brokenLinksNumber() }] broken link${ s( linksStorage.brokenLinksNumber() )} found, " +
                'see above for more details' )
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
     */
    @Requires({ pageUrl && referrerUrl && referrerContent && linksStorage && threadPool })
    void checkLinks ( String pageUrl, String referrerUrl, String referrerContent, boolean isRootLink )
    {
        final ext = ext()
        delay( ext.requestDelay )

        try
        {
            final byte[] bytes = readBytes( pageUrl, referrerUrl, referrerContent, isRootLink )
            linksProcessed.incrementAndGet()

            if ( ! bytes ) { return }

            final pageContent = new String( bytes, 'UTF-8' )

            if (( ext.ignoredContent ?: [] ).any { it( pageUrl, pageContent ) }) { return }

            final Set<String> pageLinks = readLinks( pageUrl, pageContent )
            final Set<String> newLinks  = ( pageLinks ? linksStorage.addLinksToProcess( pageLinks ) : [] )
            final processed             = linksProcessed.get()
            final queued                = threadPool.queue.size()

            linksStorage.updateBrokenLinkReferrers( pageUrl, pageLinks )

            if ( ext.linksMapFile                ) { linksStorage.updateLinksMap   ( pageUrl, pageLinks )}
            if ( ext.newLinksMapFile && newLinks ) { linksStorage.updateNewLinksMap( pageUrl, newLinks  )}

            log {
                final linksMessage    = pageLinks ? ", ${ newLinks.size() } new"     : ''
                final newLinksMessage = newLinks  ? ": ${ toMultiLines( newLinks )}" : ''

                "[$pageUrl] - ${ pageLinks.size() } link${ s( pageLinks ) } found$linksMessage, " +
                "$processed processed, $queued queued$newLinksMessage"
            }

            for ( link in newLinks )
            {
                final String linkUrl = link // Otherwise, various invocations share the same "link" instance when invoked
                futures << threadPool.submit({ checkLinks( linkUrl, pageUrl, pageContent, false )} as Runnable )
            }
        }
        catch( Throwable error )
        {
            log( LogLevel.ERROR, error ){ "Internal error while reading [$pageUrl], referrer [$referrerUrl]" }
        }
    }


    /**
     * Reads all hyperlinks in the content specified.ß
     * @param pageContent content of the page downloaded previously
     * @return all links found in the page content
     */
    @Requires({ pageUrl && pageContent })
    @Ensures({ result != null })
    List<String> readLinks ( String pageUrl, String pageContent )
    {
        final  ext          = ext()
        String cleanContent = pageContent

        if ( ext.pageTransformers )
        {
            cleanContent = ext.pageTransformers.inject( cleanContent ){ String content, Closure transformer -> transformer( content ) }
        }

        if ( ext.replaceSpecialCharacters )
        {
            cleanContent = cleanContent.replace( '\\',        '/' ).
                                        replace( '%3A',       ':' ).
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

        final links = findAll( cleanContent, ext.internalLinkPattern )

        if ( ext.checkExternalLinks ) {
            final  externalLinks = findAll ( cleanContent, ext.externalLinkPattern )
            assert externalLinks.every { it.startsWith( 'http://' ) || it.startsWith( 'https://' ) }

            links.addAll( externalLinks )
        }

        if ( ext.checkAbsoluteLinks ) {
            final  absoluteLinks = findAll ( cleanContent, ext.absoluteLinkPattern )
            assert absoluteLinks.every{ it.startsWith( '/' ) }

            links.addAll( absoluteLinks.collect{ "http://$ext.rootUrl$it".toString() })
        }

        if ( ext.checkRelativeLinks ) {

            final pageBaseUrl    = pageUrl.replaceFirst( ext.relativeLinkReminderPattern, '' )
            final requestBaseUrl = removeAllAfter( '?', pageUrl, pageBaseUrl )
            final relativeLinks  = findAll ( cleanContent, ext.relativeLinkPattern )
            assert ( ! pageBaseUrl.endsWith( '/' )) && ( ! requestBaseUrl.endsWith( '?' )) && relativeLinks.every { ! it.startsWith( '/' )}

            links.addAll( relativeLinks.collect{( it.startsWith( '?' ) ? "$requestBaseUrl$it" : "$pageBaseUrl/$it" ).toString() })
        }

        assert links.every{ it }
        final foundLinks = links.
                           collect { normalizeUrl( removeAllAfter( '#', it, it )) }.
                           toSet().
                           findAll { String link -> ( ! ( ext.ignoredLinks ?: [] ).any { it( link ) }) }.
                           sort()
        foundLinks
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
     * @return binary content of link specified or null if link shouldn't be read
     */
    @Requires({ pageUrl && referrer && referrerContent && linksStorage && ( attempt > 0 ) })
    byte[] readBytes ( final String pageUrl, final String referrer, String referrerContent, final boolean forceGetRequest, final int attempt = 1 )
    {
        final        ext             = ext()
        InputStream  inputStream     = null
        final        htmlLink        = ( ! pageUrl.toLowerCase().with{ ( ext.nonHtmlExtensions - ext.htmlExtensions ).any{ endsWith( ".$it" ) || contains( ".$it?" ) }} ) &&
                                       ( ! ( ext.nonHtmlLinks ?: [] ).any{ it( pageUrl )})
        final        readFullContent = ( htmlLink && isInternalLink ( pageUrl ))
        final        isHeadRequest   = (( ! forceGetRequest ) && ( ! readFullContent ))
        final        requestMethod   = ( isHeadRequest ? 'HEAD' : 'GET' )
        final        request         = new RequestData( pageUrl, referrer, referrerContent, linksStorage, attempt, forceGetRequest, isHeadRequest )

        try
        {
            log{ "[$pageUrl] - sending $requestMethod request .." }

            final t            = System.currentTimeMillis()
            final connection   = request.connection( openConnection( pageUrl, requestMethod ))
            inputStream        = connection.inputStream
            final byte[] bytes = ( byte[] )( isHeadRequest || readFullContent ) ?
                                    inputStream.bytes :
                                    inputStream.read().with{ ( delegate == -1 ) ? [] : [ delegate ] }

            if ( isHeadRequest ) { assert bytes.length == 0  }
            else { bytesDownloaded.addAndGet( bytes.length ) }

            log {
                "[$pageUrl] - " +
                ( readFullContent ? "[${ bytes.size()}] byte${ s( bytes.size())}" : 'can be read' ) +
                ", [${ System.currentTimeMillis() - t }] ms"
            }

            ( readFullContent && bytes ) ? decodeBytes( bytes, request ) : null
        }
        catch ( Throwable error )
        {
            return handleError( request, error )
        }
        finally
        {
            if ( inputStream ){ inputStream.close() }
        }
    }


    @Requires({ request && error })
    byte[] handleError ( RequestData request, Throwable error )
    {
        request.with {
            final ext             = ext()
            final statusCode      = ( connection ? statusCode ( connection )       : null )
            final statusCodeError = ( statusCode instanceof Throwable ? statusCode : null )
            final isRetryMatch    = ( ext.retryStatusCodes?.any { it == statusCode } ||
                                      ext.retryExceptions?. any { it.isInstance( error ) || it.isInstance( statusCodeError ) })
            final isRetry         = ( isHeadRequest || ( isRetryMatch && ( attempt < ext.retries )))
            final isAttempt       = (( ! isHeadRequest ) && ( ext.retries > 1 ) && ( isRetryMatch ))
            final logMessage      = "! [$pageUrl] - $error, status code [${ (( statusCode == null ) || statusCodeError ) ? 'unknown' : statusCode }]"

            if ( isRetry )
            {
                assert ( isHeadRequest || isAttempt )
                log { "$logMessage, ${ isHeadRequest ? 'will be retried as GET request' : 'attempt ' + attempt }" }

                delay( ext.retryDelay )
                return readBytes( pageUrl, referrer, referrerContent, true, isHeadRequest ? 1 : attempt + 1 )
            }

            logMessage = "$logMessage${ isAttempt ? ', attempt ' + attempt : '' }"

            if (( ext.ignoredBrokenLinks ?: [] ).any{ it( pageUrl, referrer, referrerContent )})
            {
                log{ "$logMessage, not registered as broken link - filtered out by ignoredBrokenLinks" }
            }
            else
            {
                log{ "$logMessage, registered as broken link" }
                linksStorage.addBrokenLink( pageUrl, referrer )
            }

            null
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
        final ext                 = ext()
        final connection          = pageUrl.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = ext.connectTimeout
        connection.readTimeout    = ext.readTimeout
        connection.requestMethod  = requestMethod

        connection.setRequestProperty( 'User-Agent' ,     ext.userAgent  )
        connection.setRequestProperty( 'Accept-Encoding', 'gzip,deflate' )
        connection.setRequestProperty( 'Connection' ,     'keep-alive'   )

        connection
    }


    @Requires({ bytes && request && request.connection })
    @Ensures ({ result && ( result.length <= bytes.length ) })
    byte[] decodeBytes( byte[] bytes, RequestData request )
    {
        final responseEncoding = request.connection.getHeaderField( 'Content-Encoding' )

        if ( ! responseEncoding ) { return bytes }

        final responseSize = Integer.valueOf( request.connection.getHeaderField( 'Content-Length' ) ?: '-1' )

        final bufferSize  = ((( responseSize > 0 ) && ( responseSize < 10 * 1024 * 1024 )) ? responseSize : 10 * 1024 )
        final inputStream = new ByteArrayInputStream( bytes ).with {
            InputStream is ->
            ( 'gzip'    == responseEncoding ) ? new GZIPInputStream( is, bufferSize ) :
            ( 'deflate' == responseEncoding ) ? new DeflaterInputStream( is, new Deflater(), bufferSize ) :
                                                null
        }

        assert inputStream, "Unknown content encoding [$responseEncoding]"
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
    String toMultiLines( Collection c, String delimiter = '*' )
    {
        "\n$delimiter [${ c.sort().join( "]\n$delimiter [" ) }]\n"
    }
}
