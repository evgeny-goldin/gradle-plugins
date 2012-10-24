package com.github.goldin.plugins.gradle.crawler

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires


/**
 * Request data container
 */
@Invariant({ pageUrl && referrer && referrerContent && linksStorage && ( attempt > 0 )})
class RequestData
{
    final String            pageUrl
    final String            referrer
    final String            referrerContent
    final LinksStorage      linksStorage
    final int               attempt
    final boolean           forceGetRequest
    final boolean           isHeadRequest
          HttpURLConnection connection


    @SuppressWarnings([ 'GroovyMethodParameterCount' ])
    RequestData ( String            pageUrl,
                  String            referrer,
                  String            referrerContent,
                  LinksStorage      linksStorage,
                  int               attempt,
                  boolean           forceGetRequest,
                  boolean           headRequest )
    {
        this.pageUrl         = pageUrl
        this.referrer        = referrer
        this.referrerContent = referrerContent
        this.linksStorage    = linksStorage
        this.attempt         = attempt
        this.forceGetRequest = forceGetRequest
        this.isHeadRequest   = headRequest
    }


    @Requires({ connection })
    @Ensures({ result == connection })
    HttpURLConnection connection( HttpURLConnection connection )
    {
        this.connection = connection
    }
}
