package com.github.goldin.plugins.gradle.common

import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires


/**
 * HTTP response data container.
 */
@Invariant({ originalUrl && actualUrl && referrer })
class HttpResponse
{
    final String       originalUrl
    final String       referrer
    final boolean      isHeadRequest

    HttpURLConnection connection
    String            actualUrl
    byte[]            data


    @Requires({ originalUrl && referrer })
    HttpResponse ( String  originalUrl,
                   String  referrer,
                   boolean isHeadRequest )
    {
        this.originalUrl   = originalUrl
        this.actualUrl     = originalUrl
        this.referrer      = referrer
        this.isHeadRequest = isHeadRequest
    }

    @Requires({ connection })
    void setConnection ( HttpURLConnection connection ) { this.connection = connection }

    @Requires({ actualUrl })
    void setActualUrl ( String actualUrl ) { this.actualUrl = actualUrl }

    @Requires({ data != null })
    void setData ( byte[] data ) { this.data = data }
}
