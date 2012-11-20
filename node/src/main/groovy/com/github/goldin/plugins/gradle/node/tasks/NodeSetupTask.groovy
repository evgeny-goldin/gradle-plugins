package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.node.ConfigHelper


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


    private void updateConfigs()
    {
        if ( ! ext.configs ) { return }

        final configHelper = new ConfigHelper( ext )

        ext.configs.each {
            Map configMap ->

            configMap.each {
                String configPath, Object configValue ->

                assert (( configValue instanceof File ) || ( configValue instanceof  Map )), \
                       "Config value for [$configPath] is of type [${ configValue?.getClass()?.name}], " +
                       "should be of type [$File.name] or [$Map.name]"

                if ( configValue instanceof File ){ configHelper.updateConfigWithFile( configPath, ( File ) configValue )}
                else                              { configHelper.updateConfigWithMap ( configPath, ( Map )  configValue )}
            }
        }
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
