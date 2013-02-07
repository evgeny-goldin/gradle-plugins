package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException


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


    @Requires({ s })
    @Ensures ({ result != null })
    private static Map<String,?> parseJsonToMap ( String s )
    {
        try { ( Map ) new JsonSlurper().parseText( s )}
        catch ( e ){ throw new GradleException( "Failed to parse and convert to map JSON [$s]", e )}
    }


    @Requires({ map != null })
    @Ensures ({ result })
    private static String stringifyMapToJson ( Map<String,?> map )
    {
        try { JsonOutput.prettyPrint( JsonOutput.toJson( map )) }
        catch ( e ) { throw new GradleException( "Failed to stringify and convert to JSON map $map", e )}
    }


    @Requires({ ( map != null ) && key })
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
            final  jsonMap = parseJsonToMap( configText )
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
            final Map<String,?> configData = ( configFile.file ? parseJsonToMap( configContent ) : [:] )
            assert ( configData || ( ! configFile.file )), "No configuration data was read from [$configFile.canonicalPath]"

            newConfigData.each {
                String key, Object value ->
                updateConfigMap( configData, key, value )
            }

            writeConfigFile( configFile, ( configFile.file && ext.configMergePreserveOrder ) ?
                                         mergeConfig ( configContent, configData ) :
                                         stringifyMapToJson ( configData ))
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
        assert parseJsonToMap( content ), "No configuration data was read from [$content]"
        task.write( configFile, content )
    }


    @Requires({ configContent && ( keys != null ) && ( configData != null ) })
    private String mergeConfig ( String configContent, List<String> keys = [], Map<String, ?> configData )
    {
        configData.inject( configContent ){
            String content, String key, Object value ->

            ( value instanceof Map ) ? mergeConfig          ( content, ( List<String> )( keys + key ), ( Map ) value ) :
                                       mergeConfigPlainValue( content, ( List<String> )( keys + key ), value )
        }
    }


    @Requires({ configContent && keys && ( ! ( value instanceof Map )) })
    private String mergeConfigPlainValue ( String configContent, List<String> keys, Object value )
    {
        String keysPattern = ''

        keys.collect { '"\\Q' + it + '\\E"' }.eachWithIndex {
            String key, int index ->

            final separator = (( index  < ( keys.size() - 2 )) ? '\\s*:.*?\\{.+?'           : // extra '{' are allowed for intermediate delimiters
                               ( index == ( keys.size() - 2 )) ? '\\s*:[^\\{]*?\\{[^\\{]+?' : // extra '{' are *not* allowed for the last delimiter
                                                                 '' )
            keysPattern    += ( key + separator )
        }

        final valuePattern = ~( '(?s)(.*\\{[^\\{]*' + keysPattern + '\\s*:\\s*)(.+?)(?=(,|\\s|\\}))' )
        final keysExist    = valuePattern.matcher( configContent ).find()

        if ( keysExist )
        {
            return configContent.replaceFirst( valuePattern ){
                final asIs = ( value == null            ) ||
                             ( value instanceof Number  ) ||
                             ( value instanceof Boolean ) ||
                             ( value as String ).with { startsWith( '"' ) || endsWith( '"' ) }

                it[ 1 ] + ( asIs ? value as String : '"' + value + '"' )
            }
        }

        switch ( ext.configsNewKeys )
        {
            case 'fail'   : throw new GradleException( "Unable to merge config keys $keys with [$configContent], value pattern [$valuePattern] can't be found" )
            case 'ignore' : return configContent
            default       : return stringifyMapToJson( updateConfigMap( parseJsonToMap( configContent ), keys.join( ext.configsKeyDelimiter ), value ))
        }
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
                case 'ignore' : break
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
