package com.github.goldin.plugins.gradle.common.helper

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.InheritConstructors
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException


@InheritConstructors
final class JsonHelper extends BaseHelper
{
    /**
     * Converts file provided to Json {@code Map}.
     */
    @Requires({ file.file })
    @Ensures ({ result != null })
    Map<String,?> jsonToMap ( File file )
    {
        jsonToObject( file.getText( 'UTF-8' ).trim(), Map, file )
    }


    /**
     * Converts content provided to Json {@code Map}.
     */
    @Requires({ content })
    @Ensures ({ result != null })
    Map<String,?> jsonToMap ( String content, File origin = null )
    {
        assert content.trim().with { startsWith( '{' ) && endsWith( '}' ) }
        jsonToObject( content, Map, origin )
    }


    /**
     * Converts content provided to Json {@code List}.
     */
    @Requires({ content })
    @Ensures ({ result != null })
    List<?> jsonToList ( String content, File origin = null )
    {
        assert content.trim().with { startsWith( '[' ) && endsWith( ']' ) }
        jsonToObject( content, List, origin )
    }


    /**
     * Converts content provided to Json object.
     */
    @Requires({ content && type })
    @Ensures ({ result != null })
    <T> T jsonToObject ( String content, Class<T> type, File origin = null )
    {
        try
        {
            new ObjectMapper().readValue( content, type )
        }
        catch ( Throwable e ){ throw new GradleException(
            """
            |Failed to parse JSON content${ origin ? ' from file:' + origin.canonicalPath : '' } to $type.simpleName
            |-----------------------------------------------
            |$content
            |-----------------------------------------------
            |Consult http://jsonlint.com/ or http://jsonformatter.curiousconcept.com/.
            """.stripMargin().toString().trim(), e )}
    }


    /**
     * Converts object provided to Json {@code String} writing it to file, if specified.
     */
    @Ensures ({ result })
    String objectToJson ( Object o, File file = null )
    {
        try
        {
            final json = new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString( o )

            if ( file )
            {
                file.parentFile.with { File f -> assert  ( f.directory || f.mkdirs()), "Failed to mkdir [$f.canonicalPath]" }
                file.write( json, 'UTF-8' )
            }

            json
        }
        catch ( e ) { throw new GradleException( "Failed to convert [$o] to JSON", e )}
    }
}
