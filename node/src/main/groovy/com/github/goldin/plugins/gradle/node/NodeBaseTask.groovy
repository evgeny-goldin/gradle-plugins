package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    final NodeHelper helper = new NodeHelper()


    @Override
    void verifyExtension( String description )
    {
        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.nodeVersion,         "'nodeVersion' should be defined in $description"
        assert ext.testCommand,         "'testCommand' should be defined in $description"
        assert ext.startCommand,        "'startCommand' should be defined in $description"
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
    }


    /**
     * Retrieves initial part of the bash script to be used by various tasks.
     */
    final String bashScript()
    {
        final setupScript = new File( scriptPath( SETUP_SCRIPT ))
        assert setupScript.file, "[$setupScript] not found"

        final binFolder = new File( NODE_MODULES_BIN )
        assert binFolder.directory, "[$binFolder] not found"

        """#!/bin/bash

        source $setupScript.canonicalPath
        export PATH=$binFolder:\$PATH

        """.stripIndent()
    }


    @Requires({ scriptName })
    @Ensures ({ result })
    final String scriptPath( String scriptName ) { "$project.buildDir/$scriptName" }


    abstract void nodeTaskAction()


    @Override
    final void taskAction()
    {
        verifyGitAvailable()
        cleanWorkspace()
        cleanNodeModules()
        updateConfigs()
        setupNode()

        nodeTaskAction()
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

        ext.configs.each {
            String configPath, Map<String,?> configValue -> updateConfig( configPath, configValue )
        }
    }


    @Requires({ configPath && newConfigData })
    @Ensures ({ new File( configPath ).with{ file && length() }})
    void updateConfig( String configPath, Map<String, ?> newConfigData )
    {
        final configFile                     = new File( configPath )
        final Map<String, Object> configData =
            ( Map ) ( configFile.file ? new JsonSlurper().parseText( configFile.getText( 'UTF-8' )) : [:] )

        newConfigData.each {
            String key, Object value -> updateConfigMap( configData, key, value )
        }

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


    private void setupNode()
    {
        final setupScriptTemplate = this.class.classLoader.getResourceAsStream( 'setup-node.sh' ).text
        final nodeVersion         = ( ext.nodeVersion == 'latest' ) ? helper.latestNodeVersion() : ext.nodeVersion
        final setupScript         = setupScriptTemplate.replace( '${nodeVersion}', nodeVersion  ).
                                                        replace( '${NODE_ENV}',    ext.NODE_ENV )

        bashExec( setupScript, scriptPath( SETUP_SCRIPT ))
    }
}
