package com.github.goldin.plugins.gradle.node

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires

import java.text.DateFormat


/**
 * Various helper methods for the Node plugin tasks.
 */
class NodeHelper
{
    NodeExtension ext
    ConfigHelper  configHelper


    @Requires({ ext })
    @Ensures({ this.ext && configHelper.ext })
    void setExt ( NodeExtension ext )
    {
        this.ext         = ext
        configHelper.ext = ext
    }


    /**
     * Retrieves latest Node.js version
     * @return latest Node.js version
     */
    @Ensures({ result })
    String latestNodeVersion(){ latestNodeVersion( 'http://nodejs.org/dist/'.toURL().text )}


    /**
     * Retrieves latest Node.js version reading the content provided.
     *
     * @param content 'http://nodejs.org/dist/' content
     * @return latest Node.js version
     */
    @Requires({ content })
    @Ensures ({ result  })
    String latestNodeVersion( String content )
    {
        // Map: release date => version
        final Map<String, String> dateToVersionMap =
            content.
            // List of Lists, l[0] is Node version, l[1] is version release date
            findAll( />(v.+?)\/<\/a>\s+(\d{2}-\w{3}-\d{4} \d{2}:\d{2})\s+-/ ){ it[ 1 .. 2 ] }.
            inject([:]){ Map m, List l -> m[ l[1] ] = l[0]; m }

        final DateFormat formatter = new java.text.SimpleDateFormat( 'dd-MMM-yyyy HH:mm' )
        final latestDate           = dateToVersionMap.keySet().max{ String date -> formatter.parse( date ).time }
        final latestVersion        = dateToVersionMap[ latestDate ]
        latestVersion
    }


    /**
     * Updates JSON config file specified using the file provided.
     *
     * @param  configPath    JSON config file to update or create if doesn't exist yet
     * @param  newConfigData config data file to read, can be another JSON config or a .properties file
     * @return               config file updated or created
     */
    @Requires({ configPath && newConfigData })
    @Ensures ({ result.with{ file && length() }})
    File updateConfig( String configPath, File newConfigData )
    {
        assert newConfigData.file, "[$newConfigData] is not available"
        final  configText = newConfigData.text.trim()
        assert configText, "[$newConfigData.canonicalPath] is empty"

        if ( configText.with{ startsWith( '{' ) and endsWith( '}' ) })
        {
            final  json = new JsonSlurper().parseText( configText )
            assert ( json instanceof Map ) && ( json ), "Failed to read JSON data from [$newConfigData.canonicalPath]"

            updateConfig( configPath, ( Map ) json )
        }
        else
        {
            final properties = new Properties()
            properties.load( new StringReader( configText ))
            assert properties, "Failed to load Properties data from [$newConfigData.canonicalPath]"

            updateConfig( configPath, ( Map ) properties )
        }
    }


    /**
     * Updates JSON config file specified using the data provided.
     *
     * @param configPath    JSON config file to update or create if doesn't exist yet
     * @param newConfigData config data to use for updating,
     *                      keys may contain {@link NodeExtension#configsKeyDelimiter} to indicate configuration nesting,
     *                      values may be real values or another configuration {@code Map} if read from JSON
     * @return              config file updated or created
     */
    @Requires({ configPath && newConfigData })
    @Ensures ({ result.with{ file && length() }})
    File updateConfig( String configPath, Map<String, ?> newConfigData )
    {
        final configFile               = new File( configPath )
        final Map<String,?> configData =
            ( Map ) ( configFile.file ? new JsonSlurper().parseText( configFile.getText( 'UTF-8' )) : [:] )

        newConfigData.each {
            String key, Object value ->
            configHelper.updateConfigMap( configData, key, value )
        }

        assert configData
        final  configDataStringified = JsonOutput.prettyPrint( JsonOutput.toJson( configData ))
        assert configDataStringified

        configFile.write( configDataStringified, 'UTF-8' )
        configFile
    }
}
