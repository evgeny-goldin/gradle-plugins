package com.github.goldin.plugins.gradle.crawler
import com.github.goldin.plugins.gradle.common.HttpResponse
import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires


/**
 * Links crawler specific HTTP response
 */
@Invariant({ referrerContent && linksStorage && ( attempt > 0 )})
class CrawlerHttpResponse extends HttpResponse
{
    final String       referrerContent
    final LinksStorage linksStorage
    final int          attempt


    @Requires({ originalUrl && referrer && referrerContent && linksStorage && ( attempt > 0 ) })
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    CrawlerHttpResponse ( String       originalUrl,
                          String       referrer,
                          boolean      isHeadRequest,
                          String       referrerContent,
                          LinksStorage linksStorage,
                          int          attempt )
    {
        super( originalUrl, referrer, isHeadRequest )

        this.referrerContent = referrerContent
        this.linksStorage    = linksStorage
        this.attempt         = attempt
    }
}
