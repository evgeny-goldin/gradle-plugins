package com.github.goldin.plugins.gradle.crawler

import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires


/**
 * Request/Response data container
 */
@Invariant({ url && referrer && referrerContent && linksStorage && ( attempt > 0 )})
class ResponseData
{
    final String       referrer
    final String       referrerContent
    final LinksStorage linksStorage
    final int          attempt
    final boolean      forceGetRequest
    final boolean      isHeadRequest

    HttpURLConnection connection
    String            url
    byte[]            data


    @SuppressWarnings([ 'GroovyMethodParameterCount' ])
    ResponseData ( String       url,
                   String       referrer,
                   String       referrerContent,
                   LinksStorage linksStorage,
                   int          attempt,
                   boolean      forceGetRequest,
                   boolean      headRequest )
    {
        this.url             = url
        this.referrer        = referrer
        this.referrerContent = referrerContent
        this.linksStorage    = linksStorage
        this.attempt         = attempt
        this.forceGetRequest = forceGetRequest
        this.isHeadRequest   = headRequest
    }

    @Requires({ connection })
    void setConnection ( HttpURLConnection connection ) { this.connection = connection }

    @Requires({ url })
    void setUrl ( String url ) { this.url = url }

    @Requires({ data != null })
    void setData ( byte[] data ) { this.data = data }
}
