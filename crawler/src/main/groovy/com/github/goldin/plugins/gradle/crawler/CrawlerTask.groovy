package com.github.goldin.plugins.gradle.crawler
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern


/**
 * {@link CrawlerPlugin} task.
 */
class CrawlerTask extends BaseTask
{
    private       ThreadPoolExecutor threadPool
    private final Object             mainThreadLock  = new Object()
    private final LinksStorage       linksStorage    = new LinksStorage()
    private final Queue<Future>      futures         = new ConcurrentLinkedQueue<Future>()
    private final AtomicLong         bytesDownloaded = new AtomicLong( 0L )


    /**
     * Retrieves current plugin extension object.
     * @return current plugin extension object
     */
    CrawlerExtension ext () { extension ( 'crawler', CrawlerExtension ) }


    String s( Collection c ){ s( c.size()) }
    String s( int        j ){ j == 1 ? '' : 's' }


    @Override
    void taskAction ()
    {
        final ext  = verifyAndUpdateExtension()
        threadPool = Executors.newFixedThreadPool( ext.threadPoolSize ) as ThreadPoolExecutor

        printStartBanner()
        submitRootLinks()
        waitForIdle()
        printReport()

        if ( linksStorage.brokenLinksNumber() && ext.failOnBrokenLinks )
        {
            throw new GradleException(
                "[${ linksStorage.brokenLinksNumber() }] broken link${ s( linksStorage.brokenLinksNumber())} found:\n" +
                toMultiLines( linksStorage.brokenLinks()))
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
        final extensionDescription = "${ CrawlerPlugin.EXTENSION_NAME } { .. }"

        assert ext.baseUrl,            "'baseUrl' should be defined in $extensionDescription"
        assert ext.threadPoolSize > 0, "'threadPoolSize' [${ ext.threadPoolSize }] in $extensionDescription should be positive"
        assert ext.connectTimeout > 0, "'connectTimeout' [${ ext.connectTimeout }] in $extensionDescription should be positive"
        assert ext.readTimeout    > 0, "'readTimeout' [${ ext.readTimeout }] in $extensionDescription should be positive"

        assert ext.relativeLinkPattern && ext.anchorPattern

        assert ( ! ext.serverAddress   ), "No 'serverAddress' should be used in $extensionDescription"
        assert ( ! ext.basePattern     ), "No 'basePattern' should be used in $extensionDescription"
        assert ( ! ext.linkPattern     ), "No 'linkPattern' should be used in $extensionDescription"
        assert ( ! ext.cleanupPatterns ), "No 'cleanupPatterns' should be used in $extensionDescription - use 'cleanupRegexes' instead"
        assert ( ! ext.ignoredPatterns ), "No 'ignoredPatterns' should be used in $extensionDescription - use 'ignoredRegexes' instead"

        ext.baseUrl         = ext.baseUrl.replaceAll( '^.*?://', '' ) // Cleaning up any protocols specified
        ext.host            = ext.host ?: ext.baseUrl
        ext.serverAddress   = ext.host.replaceAll( '(\\\\|/).*', '' )
        ext.basePattern     = Pattern.compile( /\Q${ ext.baseUrl }\E/ )
        ext.linkPattern     = Pattern.compile( /(?:'|")(https?:\/\/\Q${ ext.baseUrl }\E.*?)(?:'|")/ )
        ext.cleanupPatterns = ( ext.cleanupRegexes ?: []     ).collect { Pattern.compile( it )  }
        ext.ignoredPatterns = ( ext.ignoredRegexes ?: []     ).collect { Pattern.compile( it )  }
        ext.rootLinks       = ( ext.rootLinks      ?: [ '' ] ).collect { "http://$ext.host/$it" }

        assert ext.baseUrl && ext.host && ext.basePattern && ext.linkPattern && ext.rootLinks
        ext
    }


    /**
     * Prints startup banner.
     */
    void printStartBanner ()
    {
        final ext           = ext()
        final bannerMessage = "Checking [http://$ext.host] links with [${ ext.threadPoolSize }] thread${ s( ext.threadPoolSize ) }, verbose [$ext.verbose]"
        final bannerLine    = "-" * ( bannerMessage.size() + 2 )

        logger.info( bannerLine )
        logger.info( " $bannerMessage" )

        if ( ext.verbose )
        {
            logger.info( " Base URL             - [${ ext.baseUrl }]" )
            logger.info( " Base pattern         - [${ ext.basePattern }]" )
            logger.info( " Link pattern         - [${ ext.linkPattern }]" )
            logger.info( " Fail on broken links - [${ ext.failOnBrokenLinks }]" )
            logger.info( " Cleanup patterns     - ${ ext.cleanupPatterns }" )
            logger.info( " Ignored (contains)   - ${ ext.ignoredContains }" )
            logger.info( " Ignored (endsWith)   - ${ ext.ignoredEndsWith }" )
            logger.info( " Ignored patterns     - ${ ext.ignoredPatterns }" )
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
            futures << threadPool.submit({ checkLinks( pageUrl , 'Root link' )} as Runnable )
        }
    }


    /**
     * Blocks until there is no more activity in a thread pool, meaning all links are checked.
     */
    void waitForIdle ()
    {
        synchronized ( mainThreadLock )
        {
            while ( futures.any{ ! it.done })
            {
                mainThreadLock.wait()
            }
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
        final mbDownloaded = ( long )( bytesDownloaded.get() / ( 1024 * 1024 ))
        final kbDownloaded = ( long )( bytesDownloaded.get() / ( 1024 ))
        final downloaded   = "[${ mbDownloaded ?: kbDownloaded }] ${ mbDownloaded ? 'Mb' : 'Kb' } downloaded"

        final message = new StringBuilder().
              append( "\n\n[${ linksStorage.processedLinksNumber()}] link${ s( linksStorage.processedLinksNumber() ) } checked in ".toString()).
              append( "${( long )(( System.currentTimeMillis() - startTime ) / 1000 )} sec, ".toString()).
              append( "$downloaded:\n".toString()).
              append( toMultiLines( linksStorage.processedLinks())).
              append( "\n[${ linksStorage.brokenLinksNumber()}] broken link${ s( linksStorage.brokenLinksNumber()) } found".toString()).
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
     * <b>Invoked in a thread pool worker</b> - checks links in the page specified.
     *
     * @param pageUrl     URL of a page to check its links
     * @param referrerUrl URL of another page referring to the one being checked
     */
    void checkLinks ( String pageUrl, String referrerUrl )
    {
        try
        {
            assert pageUrl && referrerUrl && linksStorage && threadPool

            final ext          = ext()
            final byte[] bytes = readBytes( pageUrl, referrerUrl )

            if (( ! bytes ) ||
                ext.nonHtmlContains.any  { pageUrl.contains( it )} ||
                ext.nonHtmlExtensions.any{ pageUrl.endsWith( ".$it" )}){ return }

            final pageLinks = readLinks( new String( bytes, 'UTF-8' ))
            final newLinks  = linksStorage.addLinksToProcess( pageLinks )

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
            logger.error( "Failed to check links of page [$pageUrl], referrer [$referrerUrl]", error )
        }
        finally
        {   /**
             * Notifying main thread after every page checked.
             */
            synchronized ( mainThreadLock ){ mainThreadLock.notify()}
        }
    }


    /**
     * Reads all hyperlinks in the content specified.ß
     * @param pageContent content of the page downloaded previously
     * @return all links found with {@link CrawlerExtension#baseUrl} being replaced to {@link CrawlerExtension#host}
     */
    @Requires({ pageContent })
    @Ensures({ result != null })
    Collection<String> readLinks ( String pageContent )
    {
        final ext                = ext()
        final String cleanedText = ext.cleanupPatterns ?
            ext.cleanupPatterns.inject( pageContent ) { String text, Pattern p -> text.replaceAll( p, '' )} :
            pageContent

        ( cleanedText.findAll ( ext.linkPattern         ) { it[ 1 ] } +
          cleanedText.findAll ( ext.relativeLinkPattern ) { it[ 1 ] }.collect{ "http://${ ext.serverAddress }/$it" } ).
        collect { String link -> link.replaceFirst( ext.anchorPattern, '' )}.
        toSet().
        grep().
        findAll { String link -> ( ext.ignoredContains.every{ String  ignored -> ( ! link.contains( ignored ))}       )}.
        findAll { String link -> ( ext.ignoredEndsWith.every{ String  ignored -> ( ! link.endsWith( ignored ))}       )}.
        findAll { String link -> ( ext.ignoredPatterns.every{ Pattern ignored -> ( ! ignored.matcher( link ).find())} )}.
        collect { String link -> link.replaceFirst( ext.basePattern, ext.host )}
    }


    /**
     * Retrieves {@code byte[]} content of the link specified.
     *
     * @param link URL of a link to read
     * @param referrer URL of link referrer
     * @return content of link specified
     */
    @Requires({ link && referrer && linksStorage })
    @Ensures({ result != null })
    byte[] readBytes ( String link, String referrer )
    {
        final ext = ext()
        final t   = System.currentTimeMillis()

        try
        {
            if ( ext.verbose )
            {
                logger.info( "[$link] - reading .." )
            }

            final  connection         = link.toURL().openConnection()
            connection.connectTimeout = ext.connectTimeout
            connection.readTimeout    = ext.readTimeout
            final byte[] bytes        = connection.inputStream.bytes
            assert       bytes

            bytesDownloaded.addAndGet( bytes.size())

            if ( ext.verbose )
            {
                logger.info( "[$link] - [${ bytes.size()}] byte${ s( bytes.size())}, [${ System.currentTimeMillis() - t }] ms" )
            }

            bytes
        }
        catch ( Throwable error )
        {
            linksStorage.addBrokenLink( link, referrer )
            logger.warn( "! [$link] - $error, referred to by \n  [$referrer]\n" )
            new byte[ 0 ]
        }
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
