package com.github.goldin.plugins.gradle.common

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires

import java.nio.charset.Charset
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.GZIPInputStream


/**
 * HTTP response data container.
 */
@Invariant({ originalUrl && method && actualUrl })
class HttpResponse
{
    final String       originalUrl
    final String       method

    HttpURLConnection connection
    Object            statusCode     // Response status code (Integer) or an error thrown when we tried to read it
    String            actualUrl      // Different from 'originalUrl' if was request redirected
    InputStream       inputStream
    InputStream       errorStream
    byte[]            data           // Binary data as received in response
    byte[]            content        // Content decompressed from the binary data
    long              timeMillis     // Time in milliseconds it took to receive and process the response

    boolean getIsRedirect (){ originalUrl != actualUrl }


    @Requires({ url && method })
    HttpResponse ( String url, String method )
    {
        this.originalUrl = url
        this.actualUrl   = url
        this.method      = method
    }


    @Requires({ response })
    HttpResponse ( HttpResponse response )
    {
        this.originalUrl = response.originalUrl
        this.actualUrl   = response.actualUrl
        this.method      = response.method
        this.connection  = response.connection
        this.inputStream = response.inputStream
        this.data        = response.data
        this.content     = response.content
        this.timeMillis  = response.timeMillis
    }


    @Requires({ data != null })
    void setData ( byte[] data )
    {
        this.data    = data
        this.content = decodeContent()
    }


    @Requires({ connection && ( data != null ) })
    @Ensures ({ result != null })
    private byte[] decodeContent ()
    {
        final contentEncoding = connection.getHeaderField( 'Content-Encoding' )

        if ( ! ( contentEncoding && data )) { return data }

        final contentLength      = Integer.valueOf( connection.getHeaderField( 'Content-Length' ) ?: '-1' )
        final bufferSize         = ((( contentLength > 0 ) && ( contentLength < ( 100 * 1024 ))) ? contentLength : 10 * 1024 )
        final contentInputStream = new ByteArrayInputStream( data ).with {
            InputStream is ->
            ( 'gzip'    == contentEncoding ) ? new GZIPInputStream( is, bufferSize ) :
            ( 'deflate' == contentEncoding ) ? new DeflaterInputStream( is, new Deflater(), bufferSize ) :
                                               null
        }

        assert contentInputStream, "Unknown response content encoding [$contentEncoding]"
        contentInputStream.bytes
    }


    @Requires({ encoding })
    @Ensures ({ result != null })
    String asString ( String encoding = 'UTF-8' )
    {
        ( content ? new String( content, Charset.forName( encoding )) : '' )
    }


    @Requires({ response && response.connection })
    @Ensures ({ result })
    static Object statusCode( HttpResponse response )
    {
        try { response.connection.responseCode }
        catch ( Throwable error ) { error }
    }
}
