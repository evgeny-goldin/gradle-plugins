package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import static com.github.goldin.plugins.gradle.common.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import java.util.regex.Pattern


/**
 * Setups Node.js environment.
 */
class SetupTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        cleanWorkspace()

        ext.configsResult   = updateConfigs()
        ext.npmCleanInstall = ( ! project.file( NODE_MODULES_DIR ).directory )

        makeReplacements()

        removeDevDependencies( project.file( PACKAGE_JSON ))

        setupNode( ext.ensureForever )
    }


    private void cleanWorkspace()
    {
        if ( ! ext.cleanWorkspace ){ return }

        for ( command in ext.cleanWorkspaceCommands )
        {
            command.trim().tokenize().with{ List<String> l -> exec( l.head(), l.tail(), projectDir, false ) }
        }
    }


    @SuppressWarnings([ 'JavaStylePropertiesInvocation', 'GroovyGetterCallCanBePropertyAccess' ])
    @Ensures({ result != null })
    private List<Map<String, ?>> updateConfigs()
    {
        final configs = []

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

                log{ "Merging JSON config [$configFile.canonicalPath] with " +
                     ( isValueFile ? "[${ (( File ) configValue ).canonicalPath }]" : configValue.toString() ) }

                configs << updateConfigFile (
                    configFile,
                    ( isValueFile ? readConfigFile(( File ) configValue ) : ( Map ) configValue ))
            }
        }

        if ( ext.configs && ext.printConfigs ) { printConfigs() }
        configs
    }


    @Requires({ ext.configs })
    private void printConfigs()
    {
        for ( File configFile in ext.configs*.keySet().flatten()*.toString().toSet().collect{ checkFile( it )})
        {
            final configContent = ( ext.printConfigsMask ?: [] ).inject( read( configFile )){
                String content, String maskProperty ->
                content.replaceAll( ~/("\Q$maskProperty\E"\s*:\s*)([^,\r\n\}]+)/ ){ "${ it[ 1 ]}\"...\"" }
            }

            log{ """
                 |file:$configFile.canonicalPath
                 |$LOG_DELIMITER
                 |$configContent
                 |$LOG_DELIMITER
                 """.stripMargin().toString().trim() }
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

                final replaceFile = checkFile( replacePath )

                log{ "Updating [$replaceFile.canonicalPath] using replacements Map $replaces" }

                final String content = replaces.inject( read( replaceFile )) {
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


    @Requires({ packageJson })
    private void removeDevDependencies( File packageJson )
    {
        if ( ext.npmInstallDevDependencies || ( ! packageJson.file )){ return }

        final  packageJsonMap = jsonToMap( packageJson )
        if ( ! packageJsonMap.containsKey( 'devDependencies' )){ return }

        packageJsonMap.remove( 'devDependencies' )
        objectToJson( packageJsonMap, packageJson )
    }
}
