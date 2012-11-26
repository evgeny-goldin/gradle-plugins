package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import java.util.regex.Pattern
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
        makeReplacements()
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

        for ( configMap in ext.configs )
        {
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


    @SuppressWarnings([ 'BuilderMethodWithSideEffects' ])
    private void makeReplacements()
    {
        if ( ! ext.replaces ){ return }

        for ( replacesMap in ext.replaces )
        {
            replacesMap.each {
                String replacePath, Map replaces ->

                final replaceFile    = file( replacePath )
                final String content = replaces.inject( replaceFile.getText( 'UTF-8' )) {
                    String content, String replacePattern, String replaceContent ->

                    assert content && replacePattern && replaceContent, \
                           'Content to replace, matching pattern and replacement content should be defined'

                    if ( replacePattern.with { startsWith( '/' ) && endsWith( '/' ) })
                    {
                        final  pattern = replacePattern[ 1 .. -2 ]
                        assert pattern, 'Empty regex replace patterns are not allowed'
                        content.replaceAll( Pattern.compile( pattern ), replaceContent )
                    }
                    else
                    {
                        content.replace( replacePattern, replaceContent )
                    }
                }

                assert content, 'Resulting content after all replaces are made is empty'
                replaceFile.write( content, 'UTF-8' )
            }
        }
    }


    private void runSetupScript()
    {
        final setupScript = getResourceText( SETUP_SCRIPT ).replace( '${nvmRepo}',     NVM_GIT_REPO    ).
                                                            replace( '${nodeVersion}', ext.nodeVersion )
        assert ( ! setupScript.contains( '${' ))
        bashExec(  setupScript, scriptFile( SETUP_SCRIPT ), true, ext.generateOnly )
    }
}
