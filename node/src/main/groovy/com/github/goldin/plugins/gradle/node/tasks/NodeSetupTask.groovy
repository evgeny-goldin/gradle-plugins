package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException


/**
 * Setup Node.js environment.
 */
class NodeSetupTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        verifyGitAvailable()
        cleanWorkspace()
        cleanNodeModules()
        updateConfigs()
        runSetupScript()
    }



    private cleanWorkspace()
    {
        if ( ext.cleanWorkspace && ext.cleanWorkspaceCommands )
        {
            for ( command in ext.cleanWorkspaceCommands )
            {
                final commandSplit = command.trim().tokenize()
                exec( commandSplit.head(), commandSplit.tail(), project.rootDir )
            }
        }
    }


    private cleanNodeModules()
    {
        if ( ext.cleanNodeModules )
        {
            assert new File( project.rootDir, NODE_MODULES_DIR ).with { ( ! directory ) || project.delete( delegate ) }, \
                   "Failed to delete [$NODE_MODULES_DIR]"
        }
    }


    private void updateConfigs()
    {
        if ( ! ext.configs ) { return }

        ext.configs.each { String configPath, Map<String,?> configValue -> updateConfig( configPath, configValue )}
    }


    @Requires({ configPath && newConfigData })
    @Ensures ({ new File( configPath ).with{ file && length() }})
    void updateConfig( String configPath, Map<String, ?> newConfigData )
    {
        final configFile                     = new File( configPath )
        final Map<String, Object> configData =
            ( Map ) ( configFile.file ? new JsonSlurper().parseText( configFile.getText( 'UTF-8' )) : [:] )

        newConfigData.each { String key, Object value -> updateConfigMap( configData, key, value )}

        assert configData
        final  configDataStringified = JsonOutput.prettyPrint( JsonOutput.toJson( configData ))
        assert configDataStringified

        configFile.write( configDataStringified, 'UTF-8' )
    }


    @Requires({ ( map != null ) && key && ( value != null ) })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    void updateConfigMap( Map<String, Object> map, String key, Object value )
    {
        key             = key.trim()
        final delimiter = ext.configsKeyDelimiter

        if ( ! key.contains( delimiter ))
        {
            map[ key ] = value
            return
        }

        assert ( ! key.startsWith( delimiter )), \
               "Config key \"$key\" should not start with delimiter \"$delimiter\""

        assert ( ! key.endsWith( delimiter )), \
               "Config key \"$key\" should not end with delimiter \"$delimiter\""

        final  delimiterIndex = key.indexOf( delimiter )
        assert ( delimiterIndex > 0 ) && ( delimiterIndex < key.length() - delimiter.length())

        final subKey  = key.substring( 0, delimiterIndex )
        final nextKey = key.substring( delimiterIndex + delimiter.length())

        if ( map[ subKey ] == null ){ map[ subKey ] = [:] }
        final  nextMap  = map[ subKey ]
        assert nextMap != null

        if ( ! ( nextMap instanceof Map ))
        {
            throw new GradleException(
                "Unable to update $map using key \"$subKey\" (subkey of \"$key\"), " +
                "it already contains a non-Map value [$nextMap] of type [${ nextMap.getClass().name }] " +
                "keyed by the same key" )
        }

        updateConfigMap(( Map ) nextMap, nextKey, value )
    }


    private void runSetupScript()
    {
        final  setupScriptStream  = this.class.classLoader.getResourceAsStream( SETUP_SCRIPT )
        assert setupScriptStream, "Unable to load [$SETUP_SCRIPT] resource"

        final setupScriptTemplate = setupScriptStream.text
        final nodeVersion         = ( ext.nodeVersion == 'latest' ) ? helper.latestNodeVersion() : ext.nodeVersion
        final setupScript         = setupScriptTemplate.replace( '${nodeVersion}', nodeVersion  ).
                                                        replace( '${NODE_ENV}',    ext.NODE_ENV )

        bashExec( setupScript, scriptPath( SETUP_SCRIPT ), true, ext.generateOnly )
    }
}
