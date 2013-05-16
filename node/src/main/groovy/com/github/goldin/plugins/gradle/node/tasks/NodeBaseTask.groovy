package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.node.helpers.ConfigHelper
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.helpers.NodeHelper
import com.github.goldin.plugins.gradle.node.helpers.NpmCacheHelper


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    @Override
    Class extensionType (){ NodeExtension }

    @Delegate NodeHelper     nodeHelper
    @Delegate NpmCacheHelper cacheHelper
    @Delegate ConfigHelper   configHelper


    /**
     * Determines if current task requires an existence of {@link NodeExtension#scriptPath}
     */
    protected boolean requiresScriptPath(){ false }


    @Override
    void verifyUpdateExtension ( String description )
    {
        nodeHelper   = new NodeHelper( helperInitMap())
        cacheHelper  = new NpmCacheHelper( helperInitMap())
        configHelper = new ConfigHelper( helperInitMap())

        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.shell,               "'shell' should be defined in $description"
        assert ext.nodeVersion,         "'nodeVersion' should be defined in $description"
        assert ext.testCommand,         "'testCommand' should be defined in $description"
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
        assert ext.portNumber   >  0,   "'portNumber' should be positive in $description"
        assert ext.checkWait    >= 0,   "'checkWait' should not be negative in $description"
        assert ext.checkTimeout >= 0,   "'checkTimeout' should not be negative in $description"
        assert ext.redisWait    >= 0,   "'redisWait' should not be negative in $description"
        assert ext.mongoWait    >= 0,   "'mongoWait' should not be negative in $description"
        assert ext.configsNewKeys,      "'configsNewKeys' should be defined in $description"
        assert ext.xUnitReportFile,     "'xUnitReportFile' should be defined in $description"

        if ( ! ext.updated )
        {
            updateExtension()
            ext.updated = true
        }
    }


    @SuppressWarnings([ 'UseCollectMany', 'UnnecessaryObjectReferences' ])
    private void updateExtension()
    {
        updateChecks()
        ext.checkUrl = ext.checkUrl.startsWith( 'http' ) ?
            ext.checkUrl :
            "http://127.0.0.1:${ ext.portNumber }" + ( ext.checkUrl ? "/${ ext.checkUrl.replaceFirst( '^/', '' ) }"  : '' )
        assert ext.checkUrl

        ext.scriptPath = ext.scriptPath ?: ( ext.knownScriptPaths ?: [] ).find { new File( projectDir, it ).file }
        assert ( ext.scriptPath || ( ! requiresScriptPath()) || ( ext.run )), \
               "Couldn't find an application script to run! Specify 'scriptPath' in $description or use " +
               "'${ ( ext.knownScriptPaths ?: [] ).join( "', '" ) }'"

        ext.nodeVersion      = ( ext.nodeVersion == 'latest' ) ? latestNodeVersion() : ext.nodeVersion
        ext.removeColorCodes = ( ext.removeColor ? " | $REMOVE_COLOR_CODES" : '' )

        final echo      = { List<String> l -> l?.collect {[ "echo $it", "$it${ ext.removeColorCodes }" ]}?.flatten() }
        ext.before      = echo( ext.before )
        ext.after       = echo( ext.after  )
        ext.beforeStart = echo( ext.beforeStart )
        ext.beforeTest  = echo( ext.beforeTest )
        ext.afterStop   = echo( ext.afterStop )
        ext.afterTest   = echo( ext.afterTest )

        addRedis()
        addMongo()
    }


    private void updateChecks()
    {
        if ( ! ext.checks ) { return }
        final newChecks = [:]

        ext.checks.every {
            String url, List content ->
            assert url && content && ( content.size() == 2 )

            final newUrl = url.startsWith( 'http' ) ? url : "http://127.0.0.1:${ ext.portNumber }${ url.startsWith( '/' ) ? '' : '/' }${ url }"
            assert ( ! newChecks.containsValue( newUrl ))
            newChecks[ newUrl.toString() ] = content
        }

        assert newChecks && newChecks
        ext.checks
    }
}
