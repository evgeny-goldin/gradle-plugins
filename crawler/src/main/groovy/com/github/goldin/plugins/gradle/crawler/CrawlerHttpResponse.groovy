package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.HttpResponse
import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires


/**
 * Links crawler specific HTTP response
 */
@Invariant({ referrerUrl && linksStorage && ( attempt > 0 )})
class CrawlerHttpResponse extends HttpResponse
{
    final String       referrerUrl
    final LinksStorage linksStorage
    final int          attempt
    final boolean      isHeadRequest


    @Requires({ response && referrerUrl && linksStorage && ( attempt > 0 ) })
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    CrawlerHttpResponse ( HttpResponse response,
                          String       referrerUrl ,
                          LinksStorage linksStorage,
                          int          attempt )
    {
        super( response  )

        this.referrerUrl   = referrerUrl
        this.linksStorage  = linksStorage
        this.attempt       = attempt
        this.isHeadRequest = ( response.method == 'HEAD' )
    }
}
