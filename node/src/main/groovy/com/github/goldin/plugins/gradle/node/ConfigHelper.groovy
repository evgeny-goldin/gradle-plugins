package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel


/**
 * Helper class for updating config data
 */
class ConfigHelper
{
    private final NodeExtension ext
    private final BaseTask      task


    @Requires({ ext && task })
    @Ensures ({ this.ext && this.task })
    ConfigHelper ( NodeExtension ext, BaseTask task )
    {
        this.ext  = ext
        this.task = task
    }


    @SuppressWarnings([ 'GroovyEmptyStatementBody' ])
    @Requires({ content })
    @Ensures ({ result != null })
    private static Map<String,?> fromJsonToMap ( String content, String origin = null )
    {
        try { new ObjectMapper().readValue( content, Map )}
        catch ( e ){ throw new GradleException(
                     """
                     |Failed to parse the following JSON content${ origin ? ' coming from file:' + origin : '' }
                     |$LOG_DELIMITER
                     |$content
                     |$LOG_DELIMITER
                     |Consult http://jsonlint.com/.
                     """.stripMargin().toString().trim(), e )}
    }


    @Ensures ({ result })
    private static String objectToJson ( Object o )
    {
        try { new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString( o ) }
        catch ( e ) { throw new GradleException( "Failed to convert [$o] to JSON", e )}
    }


    @Requires({ ( map != null ) && key })
    @Ensures ({ result })
    private String noNewKeysErrorMessage( Map map, String key, Object value )
    {
        ">> Config Map $map contains no key \"$key\" to update it with [$value] - new config keys can't be created according to 'configsNewKeys'. " +
        "Make sure there's no typo in \"$key\" and that config file being updated already contains a value for it."
    }


    /**
     * Reads configuration data from the file provided which can be a JSON or .properties file.
     *
     * @param  configFile file to read
     * @return configuration data read from the file
     */
    @Requires({ configFile })
    @Ensures ({ result })
    Map<String, ?> readConfigFile ( File configFile )
    {
        assert configFile.file, "Config [$configFile.canonicalPath] is not available"

        final  configText = configFile.getText( 'UTF-8' ).trim()
        assert configText, "[$configFile.canonicalPath] is empty"

        if ( configText.with{ startsWith( '{' ) and endsWith( '}' ) })
        {
            final  jsonMap = fromJsonToMap( configText, configFile.canonicalPath )
            assert jsonMap, "No configuration data was read from [$configFile.canonicalPath]"
            jsonMap
        }
        else
        {
            final properties = new Properties()
            properties.load( new StringReader( configText ))
            assert properties, "Failed to read Properties from [$configFile.canonicalPath]"
            ( Map<String, ?> ) properties
        }
    }


    /**
     * Updates JSON config file specified using the data provided.
     *
     * @param configFile    JSON config file to update or create if doesn't exist yet
     * @param newConfigData config data to use for updating,
     *                      keys may contain {@link NodeExtension#configsKeyDelimiter} to indicate configuration nesting,
     *                      values may be real values or another configuration {@code Map} if read from JSON
     * @return              data of config file updated or created
     */
    @Requires({ configFile && newConfigData })
    @Ensures ({ result != null })
    Map<String,?> updateConfigFile ( File configFile, Map<String, ?> newConfigData )
    {
        try
        {
            if ( ! configFile.file )
            {
                switch ( ext.configsNewKeys )
                {
                    case 'fail'   : // fall through
                    case 'ignore' : throw new GradleException( "Config [$configFile.canonicalPath] to update is not available" )
                    default       : break // continue, new file will be created
                }
            }

            final configContent            = ( configFile.file ? configFile.getText( 'UTF-8' )   : ''  )
            final Map<String,?> configData = ( configFile.file ? fromJsonToMap( configContent, configFile.canonicalPath ) : [:] )
            assert ( configData || ( ! configFile.file )), "No configuration data was read from [$configFile.canonicalPath]"

            newConfigData.each {
                String key, Object value ->
                updateConfigMap( configData, key, value )
            }

            task.write( configFile, ( configFile.file && ext.configMergePreserveOrder ) ?
                                         mergeConfig ( configContent, configData ) :
                                         objectToJson ( configData ))
            configData
        }
        catch ( Throwable error )
        {
            throw new GradleException( "Failed to update config [$configFile.canonicalPath] with $newConfigData",
                                       error )
        }
    }


    @Requires({ configContent && ( keys != null ) && ( configData != null ) })
    private String mergeConfig ( String         configContent,
                                 Map<String, ?> configData,
                                 List<String>   keys             = [],
                                 Map<String, ?> parentConfigData = configData )
    {
        final String content = configData.values().any { it instanceof List } ?
            objectToJson( parentConfigData ) : // Merging of list values is not supported
            configData.inject( configContent ){
               String content, String key, Object value ->
               ( value instanceof Map  ) ? mergeConfig          ( content, value, ( List<String> )( keys + key ), parentConfigData ) :
                                           mergeConfigPlainValue( content, value, ( List<String> )( keys + key ))
        }

        assert fromJsonToMap( content )       // To make sure the data can be parsed back
        content
    }


    @SuppressWarnings([ 'GroovyContinue' ])
    @Requires({ configContent && keys && ( ! ( value instanceof Map )) })
    private String mergeConfigPlainValue ( String       configContent,
                                           Object       value,
                                           List<String> keys,
                                           int          startPosition = 0,
                                           int          keyIndex      = 0 )
    {
        final currentContent = configContent.substring( startPosition )
        assert currentContent.trim().startsWith( '{' )

        final keyPattern = ~/("\Q${ keys[ keyIndex ] }\E"\s*:\s*)(.*?)(?=(,|\r|\n|\}))/
        final matcher    = keyPattern.matcher( currentContent )

        while ( matcher.find())
        {
            final prefix   = matcher.group( 1 )
            final position = matcher.start()

            if ( bracesWeight( currentContent.substring( 0, position )) == 1 )
            {
                return ( keyIndex == ( keys.size() - 1 )) ?
                    // Last key, recursion stops, replacement made
                    configContent.substring( 0, startPosition ) + currentContent.substring( 0, position ) +
                    prefix + objectToJson( value ) + currentContent.substring( matcher.end()) :
                    // Recursion continues with the next key
                    mergeConfigPlainValue( configContent,
                                           value,
                                           keys,
                                           startPosition + position + prefix.size(),
                                           keyIndex + 1 )
            }
        }

        switch ( ext.configsNewKeys )
        {
            case 'fail'   : throw new GradleException( "Unable to merge config key [${ keys.join( ext.configsKeyDelimiter )}] with [$configContent], " +
                                                       "creating new keys is not allowed" )
            case 'ignore' : task.log( LogLevel.WARN ){ "Config key [${ keys.join( ext.configsKeyDelimiter )}] is ignored - " +
                                                       "not available in destination map" }
                            return configContent
            default       : return objectToJson( updateConfigMap( fromJsonToMap( configContent ), keys.join( ext.configsKeyDelimiter ), value ))
        }
    }


    @Requires({ text != null })
    private int bracesWeight ( String text )
    {
        int counter = 0
        for ( ch in text.toCharArray())
        {
            counter += ( ch == '{' ) ?  1 :
                       ( ch == '}' ) ? -1 :
                                        0
        }
        counter
    }


    /**
     * Updates configuration map using key and value provided.
     *
     * @param map   configuration map to update
     * @param key   configuration key, may contain {@link NodeExtension#configsKeyDelimiter} to indicate configuration nesting
     * @param value configuration value, may be a real value or another configuration {@code Map} if read from JSON
     */
    @Requires({ ( map != null ) && key })
    @Ensures ({ result == map })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter', 'GroovyIfStatementWithTooManyBranches' ])
    Map<String,?> updateConfigMap ( Map<String,?> map, String key, Object value )
    {
        key = key.trim()

        if ( key.contains( ext.configsKeyDelimiter ))
        {
            updateConfigMapKeyIsDelimited( map, key, value )
        }
        else if ( value instanceof Map )
        {
            updateConfigMapValueIsMap( map, key, ( Map ) value )
        }
        else if ( map.containsKey( key ))
        {
            map[ key ] = value
        }
        else
        {
            switch ( ext.configsNewKeys )
            {
                case 'fail'   : throw new GradleException( noNewKeysErrorMessage( map, key, value ))
                case 'ignore' : task.log( LogLevel.WARN ){ noNewKeysErrorMessage( map, key, value )}
                                return map
                default       : map[ key ] = value
            }
        }

        map
    }


    @Requires({ ( map != null ) && key.contains( ext.configsKeyDelimiter ) })
    @SuppressWarnings([ 'JavaStylePropertiesInvocation', 'GroovyGetterCallCanBePropertyAccess' ])
    private void updateConfigMapKeyIsDelimited ( Map<String,?> map, String key, Object value )
    {
        final delimiter = ext.configsKeyDelimiter

        assert ( ! key.startsWith( delimiter )), \
               "Config key \"$key\" should not start with delimiter \"$delimiter\""

        assert ( ! key.endsWith( delimiter )), \
               "Config key \"$key\" should not end with delimiter \"$delimiter\""

        assert ( ! ( value instanceof Map )), \
               "Key \"$key\" has a value of type [${ value.getClass().name }], " +
               "delimiter-separated keys should not have Map values"

        final  delimiterIndex = key.indexOf( delimiter )
        assert ( delimiterIndex > 0 ) && ( delimiterIndex < key.length() - delimiter.length())

        final subKey  = key.substring( 0, delimiterIndex )
        final nextKey = key.substring( delimiterIndex + delimiter.length())

        updateConfigMapValueIsMap( map, subKey, [ ( nextKey ) : value ] )
    }


    @Requires({ ( map != null ) && key && ( ! key.contains( ext.configsKeyDelimiter )) && ( value != null ) })
    @SuppressWarnings([ 'JavaStylePropertiesInvocation', 'GroovyGetterCallCanBePropertyAccess' ])
    private void updateConfigMapValueIsMap( Map<String,?> map, String key, Map<String, ?> value )
    {
        if ( map[ key ] == null )
        {
            switch ( ext.configsNewKeys )
            {
                case 'fail'   : throw new GradleException( noNewKeysErrorMessage( map, key, value ))
                case 'ignore' : task.log( LogLevel.WARN ){ noNewKeysErrorMessage( map, key, value )}
                                return
                default       : map[ key ] = [:]
            }
        }

        final Object nextMap = map[ key ]

        assert nextMap instanceof Map, \
               "Unable to update config Map $map using key \"$key\" and Map value $value, " +
               "it already contains a non-Map value [$nextMap] of type [${ nextMap.getClass().name }] " +
               "keyed by the same key"

        value.each {
            String k, Object v ->
            updateConfigMap(( Map ) nextMap, k, v )
        }
    }
}
