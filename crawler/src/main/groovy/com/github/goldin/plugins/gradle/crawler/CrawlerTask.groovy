package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream


/**
 * {@link CrawlerPlugin} task.
 */
class CrawlerTask extends BaseTask
{
    private       ThreadPoolExecutor threadPool
    private final LinksStorage       linksStorage    = new LinksStorage()
    private final Queue<Future>      futures         = new ConcurrentLinkedQueue<Future>()
    private final AtomicLong         bytesDownloaded = new AtomicLong( 0L )
    private final static String ROOT_LINK_REFERRER   = 'Root link'


    CrawlerExtension ext () { extension ( CrawlerPlugin.EXTENSION_NAME, CrawlerExtension ) }
    String s( Collection c ){ s( c.size()) }
    String s( Number     j ){ j == 1 ? '' : 's' }


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
        final extensionDescription = "${ CrawlerPlugin.EXTENSION_NAME } { .. }"

        assert ext.baseUrl,            "'baseUrl' should be defined in $extensionDescription"
        assert ext.userAgent,          "'userAgent' should be defined in $extensionDescription"
        assert ext.threadPoolSize > 0, "'threadPoolSize' [${ ext.threadPoolSize }] in $extensionDescription should be positive"
        assert ext.connectTimeout > 0, "'connectTimeout' [${ ext.connectTimeout }] in $extensionDescription should be positive"
        assert ext.readTimeout    > 0, "'readTimeout' [${ ext.readTimeout }] in $extensionDescription should be positive"

        assert ext.externalLinkPattern && ext.absoluteLinkPattern && ext.relativeLinkPattern && ext.anchorPattern && ext.protocolPattern

        assert ( ! ext.serverAddress   ), "No 'serverAddress' should be used in $extensionDescription"
        assert ( ! ext.basePattern     ), "No 'basePattern' should be used in $extensionDescription"
        assert ( ! ext.linkPattern     ), "No 'linkPattern' should be used in $extensionDescription"
        assert ( ! ext.cleanupPatterns ), "No 'cleanupPatterns' should be used in $extensionDescription - use 'cleanupRegexes' instead"
        assert ( ! ext.ignoredPatterns ), "No 'ignoredPatterns' should be used in $extensionDescription - use 'ignoredRegexes' instead"

        ext.baseUrl         = ext.baseUrl.replaceAll( ext.protocolPattern, '' )
        ext.host            = ext.host?.  replaceAll( ext.protocolPattern, '' ) ?: ext.baseUrl
        ext.serverAddress   = ext.host.replaceAll( '(\\\\|/).*', '' )
        ext.basePattern     = Pattern.compile( /\Q${ ext.baseUrl }\E/ )
        ext.linkPattern     = Pattern.compile( /(?:'|"|>)(https?:\/\/\Q${ ext.baseUrl }\E.*?)(?:'|"|<)/ )
        ext.cleanupPatterns = ( ext.cleanupRegexes ?: []     ).collect { Pattern.compile( it )  }
        ext.ignoredPatterns = ( ext.ignoredRegexes ?: []     ).collect { Pattern.compile( it )  }
        ext.rootLinks       = ( ext.rootLinks      ?: [ '' ] ).collect {
            "http://$ext.host${ (( ! it ) || ext.host.endsWith( '/' ) || it.startsWith( '/' )) ? '' : '/' }$it".toString()
        }.grep().toSet().sort()

        assert ext.rootLinks, "No root links specified in $extensionDescription to start crawling from"
        assert ext.baseUrl && ext.host && ext.basePattern && ext.linkPattern && ext.rootLinks
        assert ( ! ext.serverAddress.endsWith( '/' ))
        ext
    }


    /**
     * Prints startup banner.
     */
    void printStartBanner ()
    {
        final ext  = ext()
        final host = ext.host.replaceAll( '/.*', '' )
        assert ( ! host.contains( '/' ))

        final ipAddress     = (( ext.host =~ /^\d+/ ) ? '' : " (${ InetAddress.getByName( host ).hostAddress })" )
        final bannerMessage = "Checking [http://$ext.host]${ ipAddress } links with [${ ext.threadPoolSize }] thread${ s( ext.threadPoolSize ) }, " +
                              "verbose [$ext.verbose], " +
                              "displayLinks [${ext.displayLinks}]"
        final bannerLine    = "-" * ( bannerMessage.size() + 2 )

        logger.info( bannerLine )
        logger.info( " $bannerMessage" )

        if ( ext.verbose )
        {
            logger.info( " Root link${ s( ext.rootLinks )}:" )
            ext.rootLinks.each { logger.info( " * [$it]" )}
        }

        logger.info( bannerLine )
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
            message.append( ':\n\n' )
            for ( brokenLink in linksStorage.brokenLinks())
            {
                message.append( "- [$brokenLink] - referred to by \n  [${ linksStorage.brokenLinkReferrer( brokenLink )}]\n\n".toString())
            }
        }

        logger.info( message.toString())
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

            logger.info( "$title is written to [${ file.canonicalPath }], " +
                         "[${ linksMap.size() }] entr${ linksMap.size() == 1 ? 'y' : 'ies' }" )
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
                    "see above for more details" )
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
        try
        {
            final ext          = ext()
            final byte[] bytes = readBytes( pageUrl, referrerUrl, referrerUrl == ROOT_LINK_REFERRER )

            if ( ! bytes ) { return }

            final Set<String> pageLinks = readLinks( pageUrl, new String( bytes, 'UTF-8' ))
            final Set<String> newLinks  = linksStorage.addLinksToProcess( pageLinks )

            if ( ext.linksMapFile                ) { linksStorage.updateLinksMap   ( pageUrl, pageLinks )}
            if ( ext.newLinksMapFile && newLinks ) { linksStorage.updateNewLinksMap( pageUrl, newLinks  )}

            if ( ext.verbose )
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
            logger.error( "Failed to read [$pageUrl], referrer [$referrerUrl]", error )
        }
    }


    /**
     * Reads all hyperlinks in the content specified.ß
     * @param pageContent content of the page downloaded previously
     * @return all links found with {@link CrawlerExtension#baseUrl} being replaced to {@link CrawlerExtension#host}
     */
    @Requires({ pageUrl && pageContent })
    @Ensures({ result != null })
    List<String> readLinks ( String pageUrl, String pageContent )
    {
        final ext                = ext()
        final String cleanedText = (( String )( ext.cleanupPatterns ?
            ext.cleanupPatterns.inject( pageContent ) { String text, Pattern p -> text.replaceAll( p, '' )} :
            pageContent )).replace( '%3A', ':' ).replace( '%2F', '/' )

        final links = cleanedText.findAll ( ext.linkPattern ) { it[ 1 ] }

        if ( ext.checkExternalLinks ) {
            links.addAll( cleanedText.findAll ( ext.externalLinkPattern ) { it[ 1 ] })
        }

        if ( ext.checkAbsoluteLinks ) {
            links.addAll( cleanedText.findAll ( ext.absoluteLinkPattern ) { it[ 1 ] }.
                                      collect{ "http://${ ext.serverAddress }$it".toString() })
        }

        if ( ext.checkRelativeLinks ) {
            links.addAll( cleanedText.findAll ( ext.relativeLinkPattern ) { it[ 1 ] }.
                                      collect{ "$pageUrl${ pageUrl.endsWith( '/' ) ? '' : '/' }$it".toString() })
        }

        links.each { assert it }
        final foundLinks = links.
                           collect { it.replaceFirst( ext.anchorPattern, '' )}.
                           toSet().
                           findAll { String link -> ( ext.ignoredContains.every{ String  ignored -> ( ! link.contains( ignored ))}       )}.
                           findAll { String link -> ( ext.ignoredEndsWith.every{ String  ignored -> ( ! link.endsWith( ignored ))}       )}.
                           findAll { String link -> ( ext.ignoredPatterns.every{ Pattern ignored -> ( ! ignored.matcher( link ).find())} )}.
                           collect { String link -> link.replaceFirst( ext.basePattern, ext.host )}.
                           toSet().
                           sort()
        foundLinks
    }


    /**
     * Retrieves {@code byte[]} content of the link specified.
     *
     * @param pageUrl  URL of a link to read
     * @param referrer URL of link referrer
     * @param rootLink whether a link is a root link
     *
     * @return binary content of link specified or null if link shouldn't be read
     */
    @Requires({ pageUrl && referrer && linksStorage })
    byte[] readBytes ( String pageUrl, String referrer, boolean rootLink )
    {
        final             ext                = ext()
        HttpURLConnection connection         = null
        InputStream       inputStream        = null
        final             shouldBeDownloaded = rootLink ||
            (( pageUrl.with { contains( ext.baseUrl ) || contains( ext.host ) }) &&
             ( ! ext.nonHtmlContains.  any{ pageUrl.contains( it ) } )           &&
             ( ! ext.nonHtmlExtensions.any{ pageUrl.endsWith( ".$it" ) } ))

        try
        {
            if ( ext.verbose )
            {
                logger.info( "[$pageUrl] - reading .." )
            }

            final t            = System.currentTimeMillis()
            connection         = makeConnection( pageUrl, shouldBeDownloaded )
            inputStream        = connection.inputStream
            final byte[] bytes = inputStream.bytes
            bytesDownloaded.addAndGet( bytes.size())

            if ( ext.verbose )
            {
                logger.info( "[$pageUrl] - " +
                             ( shouldBeDownloaded ? "[${ bytes.size()}] byte${ s( bytes.size())}" : 'can be read' ) +
                             ", [${ System.currentTimeMillis() - t }] ms" )
            }

            (( shouldBeDownloaded && bytes ) ? decodeBytes( bytes, connection.getHeaderField( 'Content-Encoding' )) : null ) as byte[]
        }
        catch ( Throwable error )
        {
            final canBeIgnored = ( connection && ext.ignoredStatusCodes.any { it == connection.responseCode })
            final message      = "! [$pageUrl] - $error, " +
                                 ( canBeIgnored ? "ignored (status code is ${ connection.responseCode }), " : '' ) +
                                 "referred to by \n  [$referrer]\n"

            if ( ! canBeIgnored )
            {
                linksStorage.addBrokenLink( pageUrl, referrer )
            }

            if ( ext.verbose )
            {
                logger.warn( message )
            }

            null
        }
        finally
        {
            if ( inputStream ){ inputStream.close() }
        }
    }


    @Requires({ pageUrl })
    @Ensures({ result })
    HttpURLConnection makeConnection ( String pageUrl, boolean shouldBeDownloaded )
    {
        final ext                 = ext()
        final connection          = pageUrl.toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = ext.connectTimeout
        connection.readTimeout    = ext.readTimeout
        connection.requestMethod  = ( shouldBeDownloaded ? 'GET' : 'HEAD' )

        connection.setRequestProperty( 'User-Agent' ,     ext.userAgent  )
        connection.setRequestProperty( 'Accept-Encoding', 'gzip,deflate' )
        connection.setRequestProperty( 'Connection' ,     'keep-alive'   )

        connection
    }


    @Requires({ bytes })
    @Ensures({ result })
    byte[] decodeBytes( byte[] bytes, String contentEncoding )
    {
        if ( ! contentEncoding ) { return bytes }

        final inputStream = new ByteArrayInputStream( bytes ).with {
            InputStream is ->
            ( 'gzip'    == contentEncoding ) ? new GZIPInputStream( is ) :
            ( 'deflate' == contentEncoding ) ? new DeflaterInputStream( is ) :
                                               null
        }

        assert inputStream, "Unknown content encoding [$contentEncoding]"
        inputStream.bytes
    }


    /**
     * Converts collection specified to multiline String.
     * @param c Collection to convert.
     * @param delimiter Delimiter to use on every line.
     * @return collection specified converted to multiline String
     */
    String toMultiLines( Collection c, String delimiter = '*' )
    {
        "\n$delimiter [${ c.sort().join( "]\n$delimiter [" ) }]\n"
    }
}
