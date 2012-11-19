package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.BaseTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gcontracts.annotations.Requires


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    final NodeHelper helper = new NodeHelper()


    @Override
    void verifyExtension( String description )
    {
        assert ext.nodeVersion,  "'nodeVersion' should be defined in $description"
        assert ext.NODE_ENV,     "'NODE_ENV' should be defined in $description"
        assert ext.testCommand,  "'testCommand' should be defined in $description"
        assert ext.startCommand, "'startCommand' should be defined in $description"
    }


    /**
     * Passes a new extensions object to the closure specified.
     * Registers new extension under task's name.
     */
    @Requires({ c })
    void config( Closure c )
    {
        this.extensionName = this.name
        this.ext           = project.extensions.create( this.extensionName, NodeExtension )
        c( this.ext )
    }

    abstract void nodeTaskAction()

    @Override
    final void taskAction()
    {
        verifyGitAvailable()
        updateConfigs()
        setupNode()
        nodeTaskAction()
    }


    private void setupNode()
    {
        final setupScriptTemplate = this.class.classLoader.getResourceAsStream( 'setup-node.sh' ).text
        final nodeVersion         = ( ext.nodeVersion == 'latest' ) ? helper.latestNodeVersion() : ext.nodeVersion
        final setupScript         = setupScriptTemplate.replace( '${nodeVersion}', nodeVersion  ).
                                                        replace( '${NODE_ENV}',    ext.NODE_ENV )

        bashExec( setupScript, "$project.buildDir/${ NodeConstants.SETUP_SCRIPT }" )
    }


    private void updateConfigs()
    {
        if ( ! ext.configs ) { return }

        for ( configPath in ext.configs.keySet())
        {
            updateConfig( configPath, ( Map ) ext.configs[ configPath ] )
        }
    }


    @Requires({ configPath && newConfigData })
    private void updateConfig( String configPath, Map<String, Object> newConfigData )
    {
        final configFile                     = new File( configPath )
        final Map<String, Object> configData = ( Map ) ( configFile.file ? new JsonSlurper().parseText( configFile.getText( 'UTF-8' )) : [:] )

        for ( String configKey in newConfigData.keySet())
        {
            updateConfigMap( configData, configKey, newConfigData[ configKey ])
        }

        configFile.write( JsonOutput.prettyPrint( JsonOutput.toJson( configData )), 'UTF-8' )
    }


    @Requires({ ( map != null ) && key && ( value != null ) })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    private void updateConfigMap( Map<String, Object> map, String key, Object value )
    {
        key             = key.trim()
        final delimiter = ext.configsKeyDelimiter

        if ( ! key.contains( delimiter ))
        {
            map[ key ] = value
            return
        }

        assert ( ! key.startsWith( delimiter )), \
               "Config key [$key] should not start with delimiter [$delimiter]"

        assert ( ! key.endsWith( delimiter )), \
               "Config key [$key] should not end with delimiter [$delimiter]"

        final  delimiterIndex = key.indexOf( delimiter )
        assert ( delimiterIndex > 0 ) && ( delimiterIndex < key.length() - delimiter.length())

        final subKey  = key.substring( 0, delimiterIndex )
        final nextKey = key.substring( delimiterIndex + delimiter.length())

        if ( ! map.containsKey( subKey )){ map[ subKey ] = [:] }
        final nextMap = map[ subKey ]

        assert ( nextMap != null ) && ( nextMap instanceof Map )
        updateConfigMap(( Map ) nextMap, nextKey, value )
    }
}
