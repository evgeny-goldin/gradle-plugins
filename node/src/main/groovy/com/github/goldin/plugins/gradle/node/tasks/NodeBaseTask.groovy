package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.helpers.ConfigHelper
import com.github.goldin.plugins.gradle.node.helpers.NodeHelper
import com.github.goldin.plugins.gradle.node.helpers.NpmCacheHelper
import org.gcontracts.annotations.Requires


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
        assert ( ! projectDir.canonicalPath.find( /\s/ )), "Project directory path [${ projectDir.canonicalPath }] contains spaces - not supported!"

        nodeHelper   = new NodeHelper    ( this.project, this, this.ext )
        cacheHelper  = new NpmCacheHelper( this.project, this, this.ext )
        configHelper = new ConfigHelper  ( this.project, this, this.ext )

        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.env != null,         "'env' shouldn't be null in $description"
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
        assert ext.checks,              "'checks' should be defined in $description"

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

        ext.scriptPath = ext.scriptPath ?: ( ext.knownScriptPaths ?: [] ).find { new File( projectDir, it ).file }
        assert ( ext.scriptPath || ( ! requiresScriptPath()) || ( ext.run )), \
               "Couldn't find an application script to run! Specify 'scriptPath' in $description or use " +
               "'${ ( ext.knownScriptPaths ?: [] ).join( "', '" ) }'"

        ext.nodeVersion      = ( ext.nodeVersion == 'latest' ) ? latestNodeVersion() : ext.nodeVersion
        ext.removeColorCodes = ( ext.removeColor ? " | $REMOVE_COLOR_CODES" : '' )

        final echo       = { List<String> l -> l?.collect {[ "echo $it", "$it${ ext.removeColorCodes }" ]}?.flatten() }
        ext.before       = echo( ext.before )
        ext.after        = echo( ext.after  )
        ext.beforeStart  = echo( ext.beforeStart )
        ext.beforeTest   = echo( ext.beforeTest )
        ext.afterStop    = echo( ext.afterStop )
        ext.afterTest    = echo( ext.afterTest )
        ext.publicIp     = ext.printPublicIp ? publicIp() : ''
        ext.env.NODE_ENV = ext.NODE_ENV
        ext.env.PORT     = ext.portNumber

        // https://wiki.jenkins-ci.org/display/JENKINS/Spawning+processes+from+build
        if ( systemEnv.JENKINS_URL != null ){ ext.env.BUILD_ID = 'JenkinsLetMeSpawn' }

        addRedis()
        addMongo()
    }


    @Requires({ ext.checks })
    private void updateChecks()
    {
        final newChecks = [:]

        ext.checks.every {
            String url, List content ->
            assert url && content && ( content.size() == 2 )

            final newUrl = ( url.startsWith( 'http' ) ? url : "http://127.0.0.1:${ ext.portNumber }${ url.startsWith( '/' ) ? '' : '/' }${ url }" ).toString()
            assert ( ! newChecks.containsValue( newUrl )), "Duplicate check url [$newUrl] - mapped to ${ newChecks[ newUrl ]} and $content"
            newChecks[ newUrl ] = content
        }

        assert newChecks && ( newChecks.size() == ext.checks.size())
        ext.checks = newChecks
    }
}
