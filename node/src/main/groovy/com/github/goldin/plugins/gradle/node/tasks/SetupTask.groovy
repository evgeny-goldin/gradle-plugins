package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.node.ConfigHelper
import org.gcontracts.annotations.Ensures
import java.util.regex.Pattern


/**
 * Setup Node.js environment.
 */
class SetupTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        verifyGitAvailable()
        cleanWorkspace()
        ext.configsResult = updateConfigs()
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


    @SuppressWarnings([ 'JavaStylePropertiesInvocation', 'GroovyGetterCallCanBePropertyAccess' ])
    @Ensures({ result != null })
    private List<Map<String, ?>> updateConfigs()
    {
        final configs      = []
        final configHelper = new ConfigHelper( ext )

        for ( configMap in ( ext.configs ?: [] ))
        {
            configMap.each {
                String configPath, Object configValue ->

                final configFile  = project.file( configPath )
                final isValueFile = configValue instanceof File
                final isValueMap  = configValue instanceof Map

                assert ( isValueFile || isValueMap ), \
                       "Config value for [$configFile.canonicalPath] is of type [${ configValue?.getClass()?.name }], " +
                       "should be of type [$File.name] or [$Map.name]"

                log{ "Updating JSON config [$configFile.canonicalPath] using " +
                     ( isValueFile ? "[${ (( File ) configValue ).canonicalPath }]" : "config Map $configValue" ) }

                configs << configHelper.updateConfigFile (
                    configFile,
                    ( isValueFile ? configHelper.readConfigFile(( File ) configValue ) : ( Map ) configValue ))
            }
        }

        configs
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

                log{ "Updating [$replaceFile.canonicalPath] using replacements Map $replaces" }

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
        bashExec(  setupScript, scriptFile( SETUP_SCRIPT ), false )
    }
}
