package com.github.goldin.plugins.gradle.crawler

import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires


/**
 * Request/Response data container
 */
@Invariant({ originalUrl && referrer && referrerContent && linksStorage && ( attempt > 0 )})
class ResponseData
{
    final String       originalUrl
    final String       referrer
    final String       referrerContent
    final LinksStorage linksStorage
    final int          attempt
    final boolean      forceGetRequest
    final boolean      isHeadRequest

    HttpURLConnection connection
    String            actualUrl
    byte[]            data


    @SuppressWarnings([ 'GroovyMethodParameterCount' ])
    ResponseData ( String       originalUrl,
                   String       referrer,
                   String       referrerContent,
                   LinksStorage linksStorage,
                   int          attempt,
                   boolean      forceGetRequest,
                   boolean      headRequest )
    {
        this.originalUrl     = originalUrl
        this.referrer        = referrer
        this.referrerContent = referrerContent
        this.linksStorage    = linksStorage
        this.attempt         = attempt
        this.forceGetRequest = forceGetRequest
        this.isHeadRequest   = headRequest
    }

    @Requires({ connection })
    void setConnection ( HttpURLConnection connection ) { this.connection = connection }

    @Requires({ actualUrl })
    void setActualUrl ( String actualUrl ) { this.actualUrl = actualUrl }

    @Requires({ data != null })
    void setData ( byte[] data ) { this.data = data }
}
