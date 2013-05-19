package com.github.goldin.plugins.gradle.common.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.Project


final class JsonHelper extends BaseHelper<Object>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    JsonHelper(){ super( null, null, null )}


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    JsonHelper ( Project project, BaseTask task, Object ext ){ super( project, task, ext )}


    /**
     * Converts file provided to Json {@code Map}.
     */
    @Requires({ file.file && encoding })
    @Ensures ({ result != null })
    Map<String,?> jsonToMap ( File file, String encoding = 'UTF-8' )
    {
        jsonToMap( file.getText( encoding ).trim(), file )
    }


    /**
     * Converts content provided to Json {@code Map}.
     */
    @Requires({ content })
    @Ensures ({ result != null })
    Map<String,?> jsonToMap ( String content, File origin = null )
    {
        assert content.trim().with { startsWith( '{' ) && endsWith( '}' ) }

        final Map<?,?> map = jsonToObject( content, Map, origin )
        map.keySet().each { assert it instanceof String }
        map
    }


    /**
     * Converts content provided to Json {@code List}.
     */
    @Requires({ content && type })
    @Ensures ({ result != null })
    <T> List<T> jsonToList ( String content, Class<T> type, File origin = null )
    {
        assert content.trim().with { startsWith( '[' ) && endsWith( ']' ) }

        final List<?> list = jsonToObject( content, List, origin )
        list.each{ assert type.isInstance( it )}
        list
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
            if ( file ){ write( file, json )}
            json
        }
        catch ( e ) { throw new GradleException( "Failed to convert [$o] to JSON", e )}
    }
}
