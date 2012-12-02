package com.github.goldin.plugins.gradle.crawler

import com.github.goldin.plugins.gradle.common.HttpResponse
import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires


/**
 * Links crawler specific HTTP response
 */
@Invariant({ referrer && referrerContent && linksStorage && ( attempt > 0 )})
class CrawlerHttpResponse
{
    @Delegate final HttpResponse response
    final String       referrer
    final String       referrerContent
    final LinksStorage linksStorage
    final int          attempt
    final boolean      isHeadRequest


    @Requires({ response && referrer && referrerContent && linksStorage && ( attempt > 0 ) })
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    CrawlerHttpResponse ( HttpResponse response,
                          String       referrer,
                          String       referrerContent,
                          LinksStorage linksStorage,
                          int          attempt )
    {
        this.response        = response
        this.referrer        = referrer
        this.referrerContent = referrerContent
        this.linksStorage    = linksStorage
        this.attempt         = attempt
        this.isHeadRequest   = ( response.method == 'HEAD' )
    }
}
