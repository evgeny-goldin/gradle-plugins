package com.github.goldin.plugins.gradle.common

import org.gcontracts.annotations.Requires
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger


/**
 * {@link OutputStream} implementation logging the data sent to it to Gradle logger.
 */
class LoggingOutputStream extends OutputStream
{

    private final String        logPrefix
    private final Logger        logger
    private final LogLevel      logLevel
    private final int           endOfLine      = (( '\n' as char ) as int )
    private final int           carriageReturn = (( '\r' as char ) as int )
    private final StringBuilder wholeContent   = new StringBuilder()
    private final StringBuilder logContent     = new StringBuilder()


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
        char ch = b
        wholeContent.append( ch )
        logContent.  append( ch )
        if (( ch == endOfLine ) || ( ch == carriageReturn )){ logContent() }
    }


    private void logContent ()
    {
        logger.log( logLevel, logPrefix + logContent.toString().readLines().join( "\n$logPrefix" ))
        logContent.length = 0 // Clear StringBuilder
    }


    @Override
    void close () { if ( logContent.length()){ logContent() }}


    @Override
    String toString () { wholeContent.toString() }
}
