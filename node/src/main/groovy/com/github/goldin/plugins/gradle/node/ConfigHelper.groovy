package com.github.goldin.plugins.gradle.node

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import java.util.regex.Pattern


/**
 * Helper class for updating config data
 */
class ConfigHelper
{
    NodeExtension ext

    @Requires({ ext })
    @Ensures ({ this.ext })
    ConfigHelper ( NodeExtension ext )
    {
        this.ext = ext
    }


    @Requires({ s })
    @Ensures ({ result != null })
    private static Map<String,?> jsonToMap( String s ){ ( Map ) new JsonSlurper().parseText( s )}


    @Requires({ map != null })
    @Ensures ({ result })
    private static String mapToJson( Map<String,?> map ){ JsonOutput.prettyPrint( JsonOutput.toJson( map )) }


    @Requires({ ( map != null ) && key && ( value != null ) })
    @Ensures ({ result })
    private String noNewKeysErrorMessage( Map map, String key, Object value )
    {
        "Config Map $map contains no key \"$key\" to update it with [$value] - new config keys can't be created. " +
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
            final  jsonMap = jsonToMap( configText )
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

            final Map<String,?> configData = ( configFile.file ? jsonToMap( configFile.getText( 'UTF-8' )) : [:] )
            assert ( configData || ( ! configFile.file )), "No configuration data was read from [$configFile.canonicalPath]"

            newConfigData.each {
                String key, Object value ->
                updateConfigMap( configData, key, value )
            }

            writeConfigFile( configFile, ( configFile.file && ext.configMergePreserveOrder ) ?
                                         mergeConfig( configFile.getText( 'UTF-8' ), configData ) :
                                         mapToJson( configData ))
            configData
        }
        catch ( Throwable error )
        {
            throw new GradleException( "Failed to update config [$configFile.canonicalPath] with $newConfigData",
                                       error )
        }
    }


    @Requires({ configFile && content })
    private void writeConfigFile( File configFile, String content )
    {
        assert jsonToMap( content ), "No configuration data was read from [$content]"
        configFile.write( content, 'UTF-8' )
    }


    @Requires({ configContent && ( keys != null ) && ( configData != null ) })
    private String mergeConfig ( String configContent, List<String> keys = [], Map<String, ?> configData )
    {
        configData.inject( configContent ){
            String content, String key, Object value ->

            ( value instanceof Map ) ? mergeConfig  ( content, ( List<String> )( keys + key ), ( Map ) value ) :
                                       mergeConfigPlainValue( content, ( List<String> )( keys + key ), value )
        }
    }


    @Requires({ configContent && keys && ( value != null ) && ( ! ( value instanceof Map )) })
    private String mergeConfigPlainValue ( String configContent, List<String> keys, Object value )
    {
        final Pattern valuePattern = ~( '(?s)(.*\\{.*' +
                                        keys.collect { '"\\Q' + it + '\\E"' }.join( '\\s*:.*?\\{.+?' ) +
                                        '\\s*:\\s*)(.+?)(?=(,|\r|\n))' )

        final boolean keysExist = valuePattern.matcher( configContent ).find()

        if ( keysExist )
        {
            return configContent.replaceFirst( valuePattern ){
                it[ 1 ] + ( value instanceof Number ? value as String : '"' + value + '"' )
            }
        }

        switch ( ext.configsNewKeys )
        {
            case 'fail'   : throw new GradleException( "Unable to merge config keys $keys with [$configContent], value pattern [$valuePattern] can't be found" )
            case 'ignore' : return configContent
            default       : return mapToJson( updateConfigMap( jsonToMap( configContent ), keys.join( ext.configsKeyDelimiter ), value ))
        }
    }


    /**
     * Updates configuration map using key and value provided.
     *
     * @param map   configuration map to update
     * @param key   configuration key, may contain {@link NodeExtension#configsKeyDelimiter} to indicate configuration nesting
     * @param value configuration value, may be a real value or another configuration {@code Map} if read from JSON
     */
    @Requires({ ( map != null ) && key && ( value != null ) })
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
                case 'ignore' : break
                default       : map[ key ] = value
            }
        }

        map
    }


    @Requires({ ( map != null ) && key.contains( ext.configsKeyDelimiter ) && ( value != null ) })
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
                case 'ignore' : return
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
