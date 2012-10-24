package com.github.goldin.plugins.gradle.crawler
import org.gcontracts.annotations.Invariant


/**
 * Request data container
 */
@Invariant({ pageUrl && referrer && referrerContent && linksStorage && connection && ( attempt > 0 )})
class RequestData
{
    final String            pageUrl
    final String            referrer
    final String            referrerContent
    final LinksStorage      linksStorage
    final HttpURLConnection connection
    final int               attempt
    final boolean           forceGetRequest
    final boolean           isHeadRequest


    @SuppressWarnings([ 'GroovyMethodParameterCount' ])
    RequestData ( String            pageUrl,
                  String            referrer,
                  String            referrerContent,
                  LinksStorage      linksStorage,
                  HttpURLConnection connection,
                  int               attempt,
                  boolean           forceGetRequest,
                  boolean           headRequest )
    {
        this.pageUrl         = pageUrl
        this.referrer        = referrer
        this.referrerContent = referrerContent
        this.linksStorage    = linksStorage
        this.connection      = connection
        this.attempt         = attempt
        this.forceGetRequest = forceGetRequest
        this.isHeadRequest   = headRequest
    }
}
