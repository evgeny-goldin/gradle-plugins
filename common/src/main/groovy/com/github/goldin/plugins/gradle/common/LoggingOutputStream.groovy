package com.github.goldin.plugins.gradle.common

import org.gcontracts.annotations.Invariant
import org.gcontracts.annotations.Requires
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger


/**
 * {@link OutputStream} implementation logging the data sent to it to Gradle logger.
 */
class LoggingOutputStream extends OutputStream
{
    /**
     * Bytes collection, convertible to {@code String}.
     */
    @Invariant({ ( pointer > -1 ) && ( pointer <= bytes.length ) })
    private static class Bytes
    {
        private byte[] bytes
        private int    pointer = 0

        @Requires({ initialSize > 0 })
        Bytes ( int initialSize ){ bytes = new byte[ initialSize ]}

        @SuppressWarnings([ 'GroovySynchronizedMethod', 'GroovyPublicFieldAccessedInSynchronizedContext' ])
        synchronized void append( byte b )
        {
            assert pointer <= bytes.length
            if (   pointer == bytes.length )
            {
                byte[] newBytes = new byte[ bytes.length * 2 ]
                System.arraycopy( bytes, 0, newBytes, 0, bytes.length )
                bytes = newBytes
            }

            assert pointer < bytes.length
            bytes[ pointer++ ] = b
        }

        synchronized void reset(){ pointer = 0 }

        boolean isEmpty(){ pointer < 1 }

        @Override
        String toString() { empty ? '' : new String( bytes, 0, pointer, 'UTF-8' ).replace( '\r', '\n' )}
    }


    private final String   logPrefix
    private final Logger   logger
    private final LogLevel logLevel
    private final byte     endOfLine      = (( '\n' as char ) as byte )
    private final byte     carriageReturn = (( '\r' as char ) as byte )
    private final Bytes    wholeContent   = new Bytes( 1024 )
    private final Bytes    content        = new Bytes( 128 )


    @Requires({ ( logPrefix != null ) && logger && logLevel })
    LoggingOutputStream ( String logPrefix = '', Logger logger, LogLevel logLevel )
    {
        this.logPrefix = logPrefix
        this.logger    = logger
        this.logLevel  = logLevel
    }



    @Override
    void write ( int b )
    {
        wholeContent.append(( byte ) b )
        content.  append(( byte ) b )
        if (( b == endOfLine ) || ( b == carriageReturn )){ printContent() }
    }


    private void printContent ()
    {
        logger.log( logLevel, logPrefix + content.toString().readLines().join( "\n$logPrefix" ))
        content.reset()
    }


    @Override
    void close () { if ( ! content.empty ){ printContent() }}


    @Override
    String toString () { wholeContent.toString() }
}
