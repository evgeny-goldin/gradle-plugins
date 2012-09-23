package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


/**
 * {@link CrawlerPlugin} task.
 */
class CrawlerTask extends BaseTask
{
    private final ThreadPoolExecutor threadPool   = Executors.newFixedThreadPool( 1 ) as ThreadPoolExecutor
    private final LinksStorage       linksStorage = new LinksStorage()
    private final Queue<Future>      futures      = new ConcurrentLinkedQueue<Future>()


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
        final ext               = verifyAndUpdateExtension()
        threadPool.corePoolSize = ext.threadPoolSize

        printStartBanner()
        submitRootLinks()
        waitForIdle()
        printReport()

        if ( linksStorage.brokenLinksNumber() && ext.failOnBrokenLinks )
        {
            throw new GradleException(
                "[${ linksStorage.brokenLinksNumber() }] broken link${ s( linksStorage.brokenLinksNumber())} found" )
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

        assert ext.host,               "No 'host' defined in $extensionDescription"
        assert ext.baseUrl,            "No 'companyUrl' defined in $extensionDescription"
        assert ext.threadPoolSize > 0, "threadPoolSize [${ ext.threadPoolSize }] in $extensionDescription should be positive"
        assert ext.connectTimeout > 0, "connectTimeout [${ ext.connectTimeout }] in $extensionDescription should be positive"
        assert ext.readTimeout    > 0, "readTimeout [${ ext.readTimeout }] in $extensionDescription should be positive"

        ext.baseRegex       = /\Q${ ext.baseUrl }\E/
        ext.basePattern     = Pattern.compile( ext.baseRegex )
        ext.linkPattern     = Pattern.compile( /(?:'|")(https?:\/\/${ ext.baseRegex }.*?)(?:'|")/ )
        ext.cleanupPatterns = ( ext.cleanupRegexes ?: []     ).collect { Pattern.compile( it )  }
        ext.rootLinks       = ( ext.rootLinks      ?: [ '' ] ).collect { "http://$ext.host/$it" }

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
            logger.info( " Base URL         - [${ ext.baseUrl }]" )
            logger.info( " Base regex       - [${ ext.baseRegex }]" )
            logger.info( " Base pattern     - [${ ext.basePattern }]" )
            logger.info( " Link pattern     - [${ ext.linkPattern }]" )
            logger.info( " Cleanup patterns - ${ ext.cleanupPatterns }" )
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
        synchronized ( threadPool )
        {
            while (( threadPool.activeCount > 0 ) || ( ! threadPool.queue.empty ))
            {
                threadPool.wait()
            }
        }

        futures.each { it.get() }
        linksStorage.lock()
        threadPool.shutdown()
        threadPool.awaitTermination( 1L, TimeUnit.SECONDS )
    }


    /**
     * Prints final report after all links are checked.
     */
    void printReport ()
    {
        final message = new StringBuilder (
            "\n\n[${ linksStorage.processedLinksNumber()}] link${ s( linksStorage.processedLinksNumber() ) } checked in " +
            "${ ( System.currentTimeMillis() - startTime ) / 1000 } sec:\n" +
            toMultiLines( linksStorage.processedLinks()) +
            "\n[${ linksStorage.brokenLinksNumber()}] broken link${ s( linksStorage.brokenLinksNumber()) } found" )

        if ( linksStorage.brokenLinksNumber())
        {
            message << ':\n\n'
            for ( brokenLink in linksStorage.brokenLinks())
            {
                message << "- [$brokenLink] - referred to by \n  [${ linksStorage.brokenLinkReferrer( brokenLink )}]\n\n"
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

            if (( ! bytes ) || ext.nonHtmlExtensions.any{ pageUrl.endsWith( ".$it" )}){ return }

            final pageLinks = readLinks( new String( bytes, 'UTF-8' ))

            if ( ext.verbose )
            {
                final links           = linksStorage
                final newLinks        = pageLinks.findAll { ! links.isProcessedLink( it ) }
                final linksMessage    = pageLinks ? ", ${ newLinks.size() == 0 ? 'no' : newLinks.size()} new" : ''
                final newLinksMessage = newLinks  ? ": ${ toMultiLines( newLinks )}"                          : ''

                logger.info( "[$pageUrl] - [${ pageLinks.size() }] link${ s( pageLinks ) } found${ linksMessage } " +
                             "(${ linksStorage.processedLinksNumber() } checked so far)${ newLinksMessage }" )
            }

            for ( link in linksStorage.addLinksToProcess( pageLinks ))
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
            synchronized ( threadPool ){ threadPool.notify()}
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

        cleanedText.findAll ( ext.linkPattern ) { it[ 1 ] }.
                    toSet().
                    findAll { String link -> ( ! ext.ignoredLinks.any { String ignored -> link.endsWith( ignored )} )}.
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
                logger.info( "Reading [$link] .." )
            }

            final  connection         = link.toURL().openConnection()
            connection.connectTimeout = ext.connectTimeout
            connection.readTimeout    = ext.readTimeout
            final byte[] bytes        = connection.inputStream.bytes
            assert       bytes

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
