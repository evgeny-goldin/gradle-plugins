package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.node.ConfigHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
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
        if ( ext.cleanWorkspace )
        {
            for ( command in ext.cleanWorkspaceCommands )
            {
                final commandSplit = command.trim().tokenize()
                exec( commandSplit.head(), commandSplit.tail(), project.projectDir )
            }
        }
    }


    @SuppressWarnings([ 'JavaStylePropertiesInvocation', 'GroovyGetterCallCanBePropertyAccess' ])
    @Ensures({ result != null })
    private List<Map<String, ?>> updateConfigs()
    {
        final configs      = []
        final configHelper = new ConfigHelper( ext, this )

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

        if ( ext.configs && ext.printConfigs ) { printConfigs() }
        configs
    }


    @Requires({ ext.configs })
    private void printConfigs ()
    {
        for ( configFile in ext.configs*.keySet().flatten().toSet().collect { file( it )})
        {
            log{ """
                 |[$configFile.canonicalPath]:
                 |$LOG_DELIMITER
                 |${ configFile.getText( 'UTF-8' )}
                 |$LOG_DELIMITER
                 """.stripMargin() }
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

                log{ "Updating [$replaceFile.canonicalPath] using replacements Map $replaces" }

                final String content = replaces.inject( replaceFile.getText( 'UTF-8' )) {
                    String content, String replacePattern, Object replaceContent ->

                    assert content && replacePattern && ( replaceContent != null ), \
                           'Content to replace, matching pattern and replacement content should be defined'

                    if ( replacePattern.with { startsWith( '/' ) && endsWith( '/' ) })
                    {
                        final  pattern = replacePattern[ 1 .. -2 ]
                        assert pattern, 'Empty regex replace patterns are not allowed'
                        content.replaceAll( Pattern.compile( pattern ), replaceContent.toString())
                    }
                    else
                    {
                        content.replace( replacePattern, replaceContent.toString())
                    }
                }

                assert content, 'Resulting content after all replaces are made is empty'
                write( replaceFile, content )
            }
        }
    }


    private void runSetupScript()
    {
        final setupScript = getResourceText( 'setup.sh' ).replace( '@{nvmRepo}',            NVM_GIT_REPO    ).
                                                          replace( '@{nvmCommit}',          NVM_COMMIT      ).
                                                          replace( '@{LOG_DELIMITER}',      LOG_DELIMITER   ).
                                                          replace( '@{SCRIPT_LOCATION}',    SCRIPT_LOCATION ).
                                                          replace( '@{REMOVE_COLOR_CODES}', REMOVE_COLOR_CODES ).
                                                          replace( '@{nodeVersion}',        ext.nodeVersion )
        assert ( ! setupScript.contains( '@{' ))
        bashExec( setupScript, taskScriptFile(), false )
    }
}
