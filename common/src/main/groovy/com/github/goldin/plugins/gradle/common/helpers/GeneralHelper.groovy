package com.github.goldin.plugins.gradle.common.helpers

import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.extensions.BaseExtension
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import java.security.MessageDigest
import java.util.regex.Pattern


class GeneralHelper extends BaseHelper<BaseExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    GeneralHelper ( Project project, BaseTask task, BaseExtension ext ){ super( project, task, ext )}


    /**
     * Verifies command-line tools specified are available by executing them.
     */
    @Requires({ tools })
    void runTools ( List<String> tools )
    {
        tools.each { it.tokenize().with { List<String> list -> exec( list.head(), list.tail()) }}
    }


    @Requires({ c != null })
    @Ensures({ result != null })
    String s( Collection c, String single = '', String multiple = 's' ){ s( c.size(), single, multiple ) }


    @Ensures({ result != null })
    String s( Number j, String single = '', String multiple = 's' ){ j == 1 ? single : multiple }


    @Requires({ ( time > 0 ) && dateFormatter })
    @Ensures ({ result })
    String format ( long time ){ dateFormatter.format( new Date( time ))}

    /**
     * Sleeps for amount of milliseconds specified if positive.
     * @param millis amount of milliseconds to sleep
     */
    @Requires({ millis > -1 })
    void sleepMs( long millis )
    {
        if ( millis > 0 )
        {
            final isSeconds = (( millis % 1000 ) == 0 )
            final amount    = isSeconds ? millis / 1000 : millis
            log { "Waiting for $amount ${ isSeconds ? 'second' : 'millisecond' }${ s( amount ) } before continuing" }

            sleep( millis )
        }
    }


    /**
     * Validates XML specified with Schema provided.
     *
     * @param xml    XML to validate
     * @param schema schema to validate with
     * @return       same XML instance
     * @throws       GradleException if validation fails
     */
    @Requires({ xml && schema })
    String validateXml( String xml, String schema )
    {
        try
        {
            SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI ).
            newSchema( new StreamSource( new StringReader( schema ))).
            newValidator().
            validate( new StreamSource( new StringReader( xml )))
        }
        catch ( e )
        {
            throw new GradleException( "Failed to validate XML\n[$xml]\nusing schema\n[$schema]", e )
        }

        xml
    }


    /**
     * Retrieves all appearances of the first capturing group of the pattern specified in a String or empty list if not found.
     */
    @Requires({ s && p && ( groupIndex > -1 ) })
    @Ensures({ result != null })
    List<String> findAll( String s, Pattern p, int groupIndex = 1 ){ s.findAll ( p ) { it[ groupIndex ] }}


    /**
     * Retrieves first appearance of the first capturing group of the pattern specified in a String or null if not found.
     */
    @Requires({ s && p && ( groupIndex > 0 ) })
    String find( String s, Pattern p, int groupIndex = 1 ){ s.find ( p ) { it[ groupIndex ] }}


    /**
     * Finds a line starting with prefix specified.
     * @param prefix prefix to search for in all lines
     * @param list lines to search
     * @return line found without the prefix, trimmed or an empty String, if not found
     */
    @Requires({ prefix && ( list != null ) })
    @Ensures({ result != null })
    String find ( List<String> list, String prefix )
    {
        list.find{ it.startsWith( prefix ) }?.replace( prefix, '' )?.trim() ?: ''
    }


    /**
     * Logs message returned by the closure provided.
     *
     * @param logLevel           message log level
     * @param error              error thrown
     * @param logMessageCallback closure returning message text
     */
    @Requires({ logger && logLevel && logMessageCallback })
    String log( LogLevel logLevel = LogLevel.INFO, Throwable error = null, Closure logMessageCallback )
    {
        String logText = null

        if ( logger.isEnabled( logLevel ))
        {
            logText = logMessageCallback()
            assert logText

            if ( error ) { logger.log( logLevel, logText, error )}
            else         { logger.log( logLevel, logText )}
        }

        logText
    }


    /**
     * Throws a {@link GradleException} or logs a warning message according to {@code fail}.
     *
     * @param fail     whether execution should throw an exception
     * @param message  error message to throw or log
     * @param error    execution error, optional
     */
    @Requires({ message })
    void failOrWarn( boolean fail, String message, Throwable error = null )
    {
        if ( fail )
        {
            if ( error ) { throw new GradleException( message, error )}
            else         { throw new GradleException( message )}
        }
        else
        {
            log( LogLevel.WARN, error ){ message }
        }
    }


    /**
     * Retrieves a full path of the path provided. If relative - taken relatively to {@link org.gradle.api.Project#getProjectDir()}.
     * @param path        path to make a full path from
     * @param defaultPath default path to use if path is undefined
     * @return full path
     */
    @Requires({ path || defaultPath })
    @Ensures ({ result })
    String fullPath( String path, String defaultPath = '' )
    {
        path ? new File( path ).with{ File f -> f.absolute ? f : new File( projectDir, path ) }.canonicalPath :
               fullPath( defaultPath )
    }


    /**
     * Calculates checksum of content provided.
     */
    @Requires({ s && algorithm })
    String checksum( String s, String algorithm = 'SHA-1' )
    {
        MessageDigest.getInstance( algorithm ).digest( s.getBytes( 'UTF-8' )).
                                               inject( new StringBuilder()) {
            StringBuilder builder, byte b ->
            builder << Integer.toString(( int ) ( b & 0xff ) + 0x100, 16 ).substring( 1 )
        }.
        toString()
    }


    /**
     * Retrieves machine's local host name.
     */
    @Ensures({ result != null })
    String hostname()
    {
        try { systemEnv.COMPUTERNAME ?: systemEnv.HOSTNAME ?: exec( 'hostname' ) ?: '' }
        catch( Throwable ignored ){ 'Unknown Hostname' }
    }


    /**
     * Verifies the type of object specified and returns the object.
     */
    @Requires({ ( o != null ) && ( type != null ) })
    @Ensures ({ result.is( o ) })
    <T> T checkType( Object o, Class<T> type )
    {
        assert type.isInstance( o ), "Expected [$o] to be of type [${ type.name }] but it's of type [${ o.class.name }]"
        ( T ) o
    }
}
