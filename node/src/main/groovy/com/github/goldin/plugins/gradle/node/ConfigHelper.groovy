package com.github.goldin.plugins.gradle.node

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


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


    @Requires({ ( map != null ) && key && ( value != null ) })
    @Ensures ({ result })
    private String noNewKeysErrorMessage( Map map, String key, Object value )
    {
        "Config Map $map contains no key \"$key\" to update it with [$value] - new config keys can't be created"
    }


    /**
     * Reads configuration data from the file provided which can be a JSON or .properties file.
     *
     * @param  file file to read
     * @return configuration data read from the file
     */
    @Requires({ file.file })
    @Ensures ({ result })
    Map<String, ?> readConfigFile ( File file )
    {
        final  configText = file.getText( 'UTF-8' ).trim()
        assert configText, "[$file.canonicalPath] is empty"

        if ( configText.with{ startsWith( '{' ) and endsWith( '}' ) })
        {
            final json = new JsonSlurper().parseText( configText )
            assert ( json instanceof Map ) && ( json ), "Failed to read JSON from [$file.canonicalPath]"
            json
        }
        else
        {
            final properties = new Properties()
            properties.load( new StringReader( configText ))
            assert properties, "Failed to read Properties from [$file.canonicalPath]"
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
     * @return              config file updated or created
     */
    @Requires({ configFile && newConfigData })
    @Ensures ({ result })
    Map<String,?> updateConfigFile ( File configFile, Map<String, ?> newConfigData )
    {
        final Map<String,?> oldConfigData =
            ( Map ) ( configFile.file ? new JsonSlurper().parseText( configFile.getText( 'UTF-8' )) : [:] )

        newConfigData.each {
            String key, Object value ->
            updateConfigMap( oldConfigData, key, value )
        }

        assert oldConfigData
        final  configDataStringified = JsonOutput.prettyPrint( JsonOutput.toJson( oldConfigData ))
        assert configDataStringified

        configFile.write( configDataStringified, 'UTF-8' )
        oldConfigData
    }


    /**
     * Updates configuration map using key and value provided.
     *
     * @param map   configuration map to update
     * @param key   configuration key, may contain {@link NodeExtension#configsKeyDelimiter} to indicate configuration nesting
     * @param value configuration value, may be a real value or another configuration {@code Map} if read from JSON
     */
    @Requires({ ( map != null ) && key && ( value != null ) })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    void updateConfigMap ( Map<String,?> map, String key, Object value )
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
        else
        {
            assert (( ! ext.configsUpdateOnly ) || map.containsKey( key )), noNewKeysErrorMessage( map, key, value )
            map[ key ] = value
        }
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

        updateConfigMapValueIsMap( map, subKey, [ (nextKey) : value ] )
    }


    @Requires({ ( map != null ) && key && ( ! key.contains( ext.configsKeyDelimiter )) && ( value != null ) })
    @SuppressWarnings([ 'JavaStylePropertiesInvocation', 'GroovyGetterCallCanBePropertyAccess' ])
    private void updateConfigMapValueIsMap( Map<String,?> map, String key, Map<String, ?> value )
    {
        if ( map[ key ] == null )
        {
            assert ( ! ext.configsUpdateOnly ), noNewKeysErrorMessage( map, key, value )
            map[ key ] = [:]
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
