package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException

import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


/**
 * {@link CrawlerPlugin} task.
 */
class CrawlerTask extends BaseTask
{
    /**
     * Retrieves current plugin extension object.
     * @return current plugin extension object
     */
    private CrawlerExtension ext () { extension ( 'crawler', CrawlerExtension ) }


    private final ThreadPoolExecutor threadPool  = Executors.newFixedThreadPool( 1 ) as ThreadPoolExecutor
    private final LinksReport        linksReport = new LinksReport()


    @Override
    void taskAction ()
    {
        final ext               = verifyAndUpdateExtension()
        threadPool.corePoolSize = ext.threadPoolSize

        printStartBanner()

        for ( link in linksReport.addLinksToProcess( ext.rootLinks ))
        {
            final pageUrl     = link // Otherwise, various invocations share the same "link" instance when invoked
            final linksReport = linksReport
            threadPool.submit({ checkLinks( pageUrl, 'Root link' )} as Runnable )
        }

        synchronized ( threadPool )
        {
            while ( threadPool.activeCount || ( ! threadPool.queue.empty ))
            {
                threadPool.wait()
            }
        }

        linksReport.lock()
        threadPool.shutdown()
        threadPool.awaitTermination( 1L, TimeUnit.SECONDS )

        printReport( ext.failOnBrokenLinks )
    }


    private CrawlerExtension verifyAndUpdateExtension ()
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


    void checkLinks ( String pageUrl, String referrerUrl )
    {
        try
        {
            assert pageUrl && referrerUrl && linksReport && threadPool

            final ext = ext()
            if ( ext.ignoredLinks.any { pageUrl.endsWith( it )}) { return }

            final byte[] bytes = readBytes( pageUrl, referrerUrl )

            if (( ! bytes ) || ext.nonHtmlExtensions.any{ pageUrl.endsWith( ".$it" )}){ return }

            final pageLinks = readLinks( new String( bytes, 'UTF-8' ))

            if ( ext.verbose )
            {
                final links           = linksReport
                final newLinks        = pageLinks.findAll { ! links.isProcessedLink( it ) }
                final linksMessage    = pageLinks ? ", ${ newLinks.size() == 0 ? 'no' : newLinks.size()} new" : ''
                final newLinksMessage = newLinks  ? ": ${ toMultiLines( newLinks )}"                          : ''

                logger.info( "[$pageUrl] - [${ pageLinks.size() }] link${ s( pageLinks ) } found${ linksMessage } " +
                             "(${ linksReport.processedLinksNumber() } checked so far)${ newLinksMessage }" )
            }

            for ( link in linksReport.addLinksToProcess( pageLinks ))
            {
                final String linkUrl = link // Otherwise, various invocations share the same "link" instance when invoked
                threadPool.submit({ checkLinks( linkUrl, pageUrl )} as Runnable )
            }
        }
        catch( Throwable error )
        {
            logger.error( "Failed to check links of page [$pageUrl], referrer [$referrerUrl]", error )
        }
        finally
        {
            synchronized ( threadPool ){ threadPool.notify()}
        }
    }


    @Requires({ pageContent })
    private Collection<String> readLinks ( String pageContent )
    {
        final ext         = ext()
        final String text = ext.cleanupPatterns ?
            ext.cleanupPatterns.inject( pageContent ) { String text, Pattern p -> text.replaceAll( p, '' )} :
            pageContent

        text.findAll ( ext.linkPattern ) { it[ 1 ] }.
             toSet().
             collect { String link -> link.replaceFirst( ext.basePattern, ext.host )}
    }



    @Requires({ link && referrer && linksReport })
    @Ensures({ result != null })
    private byte[] readBytes ( String link, String referrer )
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

            if ( ext.verbose )
            {
                logger.info( "[$link]: Ok - [${ bytes.size()}] bytes, [${ System.currentTimeMillis() - t }] ms" )
            }

            bytes
        }
        catch ( Throwable error )
        {
            linksReport.addBrokenLink( link, referrer )
            logger.warn( "[$link]: Failed - $error (referrer [$referrer])" )
            new byte[ 0 ]
        }
    }


    void printStartBanner ()
    {
        final ext           = ext()
        final bannerMessage = "Checking [http://$ext.host] links with [${ ext.threadPoolSize }] thread${ s( ext.threadPoolSize ) }, verbose [$ext.verbose]"
        final bannerLine    = "-" * ( bannerMessage.size() + 2 )

        logger.info( bannerLine )
        logger.info( " $bannerMessage" )
        logger.info( " Root link${ s( ext.rootLinks )}:" )
        ext.rootLinks.each { logger.info( " * [$it]" )}
        logger.info( bannerLine )
    }


    void printReport ( boolean failOnBrokenLinks )
    {
        final message = new StringBuilder (
            "\n[${ linksReport.processedLinksNumber()}] link${ s( linksReport.processedLinksNumber() ) } checked in " +
            "${ ( System.currentTimeMillis() - startTime ) / 1000 } sec:\n" +
            toMultiLines( linksReport.processedLinks()) +
            "\n[${ linksReport.brokenLinksNumber()}] broken link${ s( linksReport.brokenLinksNumber()) } found" )

        if ( linksReport.brokenLinksNumber())
        {
            message << ':\n\n'
            for ( brokenLink in linksReport.brokenLinks())
            {
                message << "- [$brokenLink] - referred to by \n  ${ linksReport.brokenLinkReferrers( brokenLink ) }\n\n"
            }
        }

        logger.info( message.toString())

        if ( linksReport.brokenLinksNumber() && failOnBrokenLinks )
        {
            throw new GradleException( "[${ linksReport.brokenLinksNumber() }] broken link${ s( linksReport.brokenLinksNumber())} found" )
        }
    }


    String toMultiLines( Collection c, String delimiter = '*' )
    {
        "\n$delimiter [${ c.sort().join( "]\n$delimiter [" ) }]\n"
    }

    private String s( Collection c ){ s( c.size()) }
    private String s( int        j ){ j == 1 ? '' : 's' }

}
