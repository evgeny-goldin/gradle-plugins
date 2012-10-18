package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException

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
    private       ThreadPoolExecutor threadPool
    private       String             extensionName   = CrawlerPlugin.EXTENSION_NAME
    private final LinksStorage       linksStorage    = new LinksStorage()
    private final Queue<Future>      futures         = new ConcurrentLinkedQueue<Future>()
    private final AtomicLong         bytesDownloaded = new AtomicLong( 0L )
    private final static String ROOT_LINK_REFERRER   = 'Root link'

    CrawlerExtension ext () { extension ( this.extensionName, CrawlerExtension ) }


    /**
     * Passes a new extensions object to the closure specified to use it later.
     */
    void config( Closure c ){
        this.extensionName = this.name
        final extension    = new CrawlerExtension()
        project.extensions.add( extensionName, extension )
        c( extension )
    }


    String referredByMessage( String referrer ){ "referred to by\n  [$referrer]" }
    void delay( long delayInMilliseconds )     { if ( delayInMilliseconds > 0 ){ sleep( delayInMilliseconds )}}


    @Override
    void taskAction ()
    {
        final ext  = verifyAndUpdateExtension()
        threadPool = Executors.newFixedThreadPool( ext.threadPoolSize ) as ThreadPoolExecutor

        printStartBanner()
        submitRootLinks()
        waitForIdle()
        printReport()
        writeLinksMapFiles()
        checkIfBuildShouldFail()
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

        assert ext.externalLinkPattern && ext.absoluteLinkPattern && ext.relativeLinkPattern && ext.anchorPattern

        assert ( ! ext.rootUrl             ), "'rootUrl' should not be used in $extensionDescription - private area"
        assert ( ! ext.internalLinkPattern ), "'internalLinkPattern' should not be used in $extensionDescription - private area"

        ext.baseUrl             = ext.baseUrl?.trim()?.replace( '\\', '/' )?.replaceAll( '^.+?:/+', '' ) // Protocol part removed
        ext.rootUrl             = ext.baseUrl?.replaceAll( '/.*', '' )                                   // Path part removed
        ext.internalLinkPattern = Pattern.compile( /(?:'|"|>)(https?:\/\/\Q${ ext.baseUrl }\E.*?)(?:'|"|<)/ )

        assert ext.baseUrl, "'baseUrl' should be defined in $extensionDescription"
        assert ext.rootUrl && ( ! ext.rootUrl.endsWith( '/' )) && ext.internalLinkPattern

        assert ext.userAgent,           "'userAgent' should be defined in $extensionDescription"
        assert ext.threadPoolSize >  0, "'threadPoolSize' [${ ext.threadPoolSize }] in $extensionDescription should be positive"
        assert ext.connectTimeout >  0, "'connectTimeout' [${ ext.connectTimeout }] in $extensionDescription should be positive"
        assert ext.readTimeout    >  0, "'readTimeout' [${ ext.readTimeout }] in $extensionDescription should be positive"
        assert ext.retries        > -1, "'retries' [${ ext.retries }] in $extensionDescription should not be negative"
        assert ext.retryDelay     > -1, "'retryDelay' [${ ext.retryDelay }] in $extensionDescription should not be negative"
        assert ext.requestDelay   > -1, "'requestDelay' [${ ext.requestDelay }] in $extensionDescription should not be negative"

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
        if ( logger.infoEnabled )
        {
            final ext  = ext()

            final ipAddress     = (( ext.rootUrl ==~ /^\d+\.\d+\.\d+\.\d+$/ ) ? '' : " (${ InetAddress.getByName( ext.rootUrl ).hostAddress })" )
            final bannerMessage = "Checking [$ext.baseUrl]${ ipAddress } links with [${ ext.threadPoolSize }] thread${ s( ext.threadPoolSize ) }"
            final bannerLine    = "-" * ( bannerMessage.size() + 2 )

            logger.info( bannerLine )
            logger.info( " $bannerMessage" )
            logger.info( " Root link${ s( ext.rootLinks )}:" )
            ext.rootLinks.each { logger.info( " * [$it]" )}
            logger.info( bannerLine )
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
            futures << threadPool.submit({ checkLinks( pageUrl, ROOT_LINK_REFERRER )} as Runnable )
        }
    }


    /**
     * Blocks until there is no more activity in a thread pool, meaning all links are checked.
     */
    void waitForIdle ()
    {
        while ( futures.any{ ! it.done })
        {
            sleep( 5000 )
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
        final ext          = ext()
        final mbDownloaded = ( long )( bytesDownloaded.get() / ( 1024 * 1024 ))
        final kbDownloaded = ( long )( bytesDownloaded.get() / ( 1024 ))
        final downloaded   = "[${ mbDownloaded ?: kbDownloaded }] ${ mbDownloaded ? 'Mb' : 'Kb' } downloaded"

        final message = new StringBuilder().
              append( "\n\n[${ linksStorage.processedLinksNumber()}] link${ s( linksStorage.processedLinksNumber() ) } checked in ".toString()).
              append( "${( long )(( System.currentTimeMillis() - startTime ) / 1000 )} sec, ".toString()).
              append( downloaded.toString())

        if ( ext.displayLinks )
        {
            message.append( ':\n' ).append( toMultiLines( linksStorage.processedLinks()))
        }

        message.append( "\n[${ linksStorage.brokenLinksNumber()}] broken link${ s( linksStorage.brokenLinksNumber()) } found".toString()).
                append( linksStorage.brokenLinksNumber() ? '' : ' - congratulations :)' )

        if ( linksStorage.brokenLinksNumber())
        {
            message << ':\n\n'
            for ( brokenLink in linksStorage.brokenLinks())
            {
                message << "- [$brokenLink] - ${ referredByMessage( linksStorage.brokenLinkReferrer( brokenLink ))}\n\n"
            }
        }

        ( linksStorage.brokenLinksNumber() ? logger.&error :
          ext.printSummary                 ? logger.&warn  :
                                             logger.&info )( message.toString())
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

            if ( logger.infoEnabled )
            {
                logger.info( "$title is written to [${ file.canonicalPath }], [${ linksMap.size() }] entr${ linksMap.size() == 1 ? 'y' : 'ies' }" )
            }
        }

        if ( ext.linksMapFile    ) { print( ext.linksMapFile,    linksStorage.linksMap(),    'Links map'     )}
        if ( ext.newLinksMapFile ) { print( ext.newLinksMapFile, linksStorage.newLinksMap(), 'New links map' )}
    }


    /**
     * Checks if build should fail and fails it if required.
     */
    void checkIfBuildShouldFail ( )
    {
        final ext   = ext()
        final links = linksStorage.processedLinksNumber()
        final bytes = bytesDownloaded.get()

        if ( linksStorage.brokenLinksNumber() && ext.failOnBrokenLinks )
        {
            throw new GradleException(
                    "[${ linksStorage.brokenLinksNumber() }] broken link${ s( linksStorage.brokenLinksNumber() )} found, " +
                    'see above for more details' )
        }

        if ( links < ext.minimumLinks )
        {
            throw new GradleException(
                    "Only [$links] link${ s( links )} checked, [${ ext.minimumLinks }] link${ s( ext.minimumLinks )} at least required." )
        }

        if ( bytes < ext.minimumBytes )
        {
            throw new GradleException(
                    "Only [$bytes] byte${ s( bytes )} downloaded, [${ ext.minimumBytes }] byte${ s( ext.minimumBytes )} at least required." )
        }
    }


    /**
     * <b>Invoked in a thread pool worker</b> - checks links in the page specified.
     *
     * @param pageUrl     URL of a page to check its links
     * @param referrerUrl URL of another page referring to the one being checked
     */
    @Requires({ pageUrl && referrerUrl && linksStorage && threadPool })
    void checkLinks ( String pageUrl, String referrerUrl )
    {
        final ext = ext()
        delay( ext.requestDelay )

        try
        {
            final byte[] bytes = readBytes( pageUrl, referrerUrl, referrerUrl == ROOT_LINK_REFERRER )

            if ( ! bytes ) { return }

            final Set<String> pageLinks = readLinks( pageUrl, new String( bytes, 'UTF-8' ))
            final Set<String> newLinks  = ( pageLinks ? linksStorage.addLinksToProcess( pageLinks ) : [] )

            if ( ext.linksMapFile                ) { linksStorage.updateLinksMap   ( pageUrl, pageLinks )}
            if ( ext.newLinksMapFile && newLinks ) { linksStorage.updateNewLinksMap( pageUrl, newLinks  )}

            if ( logger.infoEnabled )
            {
                final linksMessage    = pageLinks ? ", ${ newLinks ? newLinks.size() : 'no' } new" : ''
                final newLinksMessage = newLinks  ? ": ${ toMultiLines( newLinks )}"               : ''

                logger.info( "[$pageUrl] - [${ pageLinks.size() }] link${ s( pageLinks ) } found${ linksMessage } " +
                             "(${ linksStorage.processedLinksNumber() } checked so far)${ newLinksMessage }" )
            }

            for ( link in newLinks )
            {
                final String linkUrl = link // Otherwise, various invocations share the same "link" instance when invoked
                futures << threadPool.submit({ checkLinks( linkUrl, pageUrl )} as Runnable )
            }
        }
        catch( Throwable error )
        {
            logger.error( "Interanl error while reading [$pageUrl], referrer [$referrerUrl]", error )
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
        final ext   = ext()
        // noinspection GroovyAssignmentToMethodParameter
        pageContent =
            (( String ) ext.pageTransformers.inject( pageContent ){ String content, Closure transformer -> transformer( content ) }).
            replace( '%3A', ':' ).
            replace( '\\',  '/' ).
            replace( '%2F', '/' )

        final links = findAll( pageContent, ext.internalLinkPattern )

        if ( ext.checkExternalLinks ) {
            final  externalLinks = findAll ( pageContent, ext.externalLinkPattern )
            assert externalLinks.every { it.startsWith( 'http://' ) || it.startsWith( 'https://' ) }

            links.addAll( externalLinks )
        }

        if ( ext.checkAbsoluteLinks ) {
            final  absoluteLinks = findAll ( pageContent, ext.absoluteLinkPattern )
            assert absoluteLinks.every{ it.startsWith( '/' ) }

            links.addAll( absoluteLinks.collect{ "http://$ext.rootUrl$it".toString() })
        }

        if ( ext.checkRelativeLinks ) {
            final pageBaseUrl   = pageUrl.replaceFirst( ext.relativeLinkReminderPattern, '' )
            final relativeLinks = findAll ( pageContent, ext.relativeLinkPattern )
            assert ( ! pageBaseUrl.endsWith( '/' )) && relativeLinks.every { ! it.startsWith( '/' )}

            links.addAll( relativeLinks.collect{ "$pageBaseUrl/$it".toString() })
        }

        assert links.every{ it }
        final foundLinks = links.
                           collect { it.replaceFirst( ext.anchorPattern, '' )}.
                           toSet().
                           findAll { String link -> ( ! ext.ignoredLinks.any { Closure c -> c( link ) }) }.
                           sort()
        foundLinks
    }


    /**
     * Retrieves {@code byte[]} content of the link specified.
     *
     * @param pageUrl         URL of a link to read
     * @param referrer        URL of link referrer
     * @param forceGetRequest whether a link should be GET-requested in any case
     *
     * @return binary content of link specified or null if link shouldn't be read
     */
    @Requires({ pageUrl && referrer && linksStorage && ( attempt > 0 ) })
    byte[] readBytes ( final String pageUrl, final String referrer, final boolean forceGetRequest, final int attempt = 1 )
    {
        final        ext           = ext()
        InputStream  inputStream   = null
        RequestData  request       = null
        final        nonHtmlLink   = ext.nonHtmlExtensions.any{ pageUrl.endsWith( ".$it" ) || pageUrl.contains( ".$it?" ) }
        final        internalLink  = ext.isInternalLink ( pageUrl )
        final        isHeadRequest = (( ! forceGetRequest ) && ( nonHtmlLink || ( ! internalLink )))
        final        requestMethod = ( isHeadRequest ? 'HEAD' : 'GET' )

        try
        {
            if ( logger.infoEnabled ){ logger.info( "[$pageUrl] - sending $requestMethod request .." )}

            final t                = System.currentTimeMillis()
            final connection       = openConnection( pageUrl, requestMethod )
            request                = new RequestData( pageUrl, referrer, linksStorage, connection, attempt, forceGetRequest, isHeadRequest )
            inputStream            = connection.inputStream
            final byte[] bytes     = (( isHeadRequest || internalLink ) ? inputStream.bytes : [ inputStream.read() ]) as byte[]
            final responseEncoding = connection.getHeaderField( 'Content-Encoding' )
            final responseSize     = Integer.valueOf( connection.getHeaderField( 'Content-Length' ) ?: '-1' )

            if ( isHeadRequest ) { assert bytes.length == 0  }
            else { bytesDownloaded.addAndGet( bytes.length ) }

            if ( logger.infoEnabled )
            {
                logger.info( "[$pageUrl] - " +
                             (( isHeadRequest || ( ! internalLink )) ? 'can be read' : "[${ bytes.size()}] byte${ s( bytes.size())}" ) +
                             ", [${ System.currentTimeMillis() - t }] ms" )
            }

            ( bytes && internalLink ) ? decodeBytes( bytes, responseEncoding, responseSize ) : null
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
            final statusCode      = statusCode ( connection )
            final statusCodeError = ( statusCode instanceof Throwable ? statusCode : null )
            final isRetryMatch    = ( ext.retryStatusCodes?.any { it == statusCode } ||
                                      ext.retryExceptions?. any { it.with { isInstance( error ) || isInstance( statusCodeError ) }} )
            final isRetry         = ( isHeadRequest || ( isRetryMatch && ( attempt < ext.retries )))
            final isAttempt       = (( ! isHeadRequest ) && ( ext.retries > 1 ) && ( isRetryMatch ))

            if ( logger.infoEnabled )
            {
                final message = "! [$pageUrl] - $error, status code [${ statusCodeError ? 'unknown' : statusCode }], " +
                                ( isRetry && isHeadRequest ? 'will be retried as GET request, ' : '' ) +
                                ( isAttempt                ? "attempt $attempt, "               : '' ) +
                                ( ! isRetry                ? 'registered as broken link, '      : '' ) +
                                referredByMessage( referrer )

                logger.warn( message )
            }

            if ( isRetry )
            {
                delay( ext.retryDelay )
                return readBytes( pageUrl, referrer, true, isHeadRequest ? 1 : attempt + 1 )
            }

            linksStorage.addBrokenLink( pageUrl, referrer )
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


    @Requires({ bytes })
    @Ensures({ result })
    byte[] decodeBytes( byte[] bytes, String responseEncoding, int responseSize )
    {
        if ( ! responseEncoding ) { return bytes }

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
     * @return collection specified converted to multi-line String
     */
    String toMultiLines( Collection c, String delimiter = '*' )
    {
        "\n$delimiter [${ c.sort().join( "]\n$delimiter [" ) }]\n"
    }
}
