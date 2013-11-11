package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.ShellHelper
import com.github.goldin.plugins.gradle.common.node.NodeSetupHelper
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.helpers.ConfigHelper
import com.github.goldin.plugins.gradle.node.helpers.DBHelper
import com.github.goldin.plugins.gradle.node.helpers.NodeHelper
import org.gcontracts.annotations.Requires


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    String group = 'Node'

    @Override
    Class<NodeExtension> extensionType(){ NodeExtension }

    @Delegate ShellHelper     shellHelper
    @Delegate NodeSetupHelper nodeSetupHelper
    @Delegate NodeHelper      nodeHelper
    @Delegate DBHelper        dbHelper
    @Delegate ConfigHelper    configHelper


    /**
     * Determines if current task requires an existence of {@link NodeExtension#scriptPath}
     */
    protected boolean requiresScriptPath(){ false }


    @Override
    void verifyUpdateExtension ( String description )
    {
        assert ( ! projectDir.canonicalPath.find( /\s/ )), "Project directory path [${ projectDir.canonicalPath }] contains spaces - currently not supported"

        shellHelper     = new ShellHelper    ( this.project, this, this.ext )
        nodeSetupHelper = new NodeSetupHelper( this.project, this, this.ext )
        nodeHelper      = new NodeHelper     ( this.project, this, this.ext )
        dbHelper        = new DBHelper       ( this.project, this, this.ext )
        configHelper    = new ConfigHelper   ( this.project, this, this.ext )

        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.env != null,         "'env' shouldn't be null in $description"
        assert ext.shell,               "'shell' should be defined in $description"
        assert ext.nodeVersion,         "'nodeVersion' should be defined in $description"
        assert ext.testCommand,         "'testCommand' should be defined in $description"
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
        assert ext.port         >  0,   "'port' should be positive in $description"
        assert ext.checkWait    >= 0,   "'checkWait' should not be negative in $description"
        assert ext.checkTimeout >= 0,   "'checkTimeout' should not be negative in $description"
        assert ext.redisWait    >= 0,   "'redisWait' should not be negative in $description"
        assert ext.mongoWait    >= 0,   "'mongoWait' should not be negative in $description"
        assert ext.configsNewKeys,      "'configsNewKeys' should be defined in $description"
        assert ext.xUnitReportFile,     "'xUnitReportFile' should be defined in $description"
        assert ext.checks,              "'checks' should be defined in $description"
        assert ext.redisListeners,      "'redisListeners' should be defined in $description"
        assert ext.mongoListeners,      "'mongoListeners' should be defined in $description"

        if ( ! ext.updated )
        {
            updateExtension()
            ext.updated = true
        }

        assert ( ext.scriptPath || ( ! requiresScriptPath()) || ( ext.run )), \
               "Couldn't find an application script to run! Specify 'scriptPath' in $description or use " +
               "'${ ( ext.knownScriptPaths ?: [] ).join( "', '" ) }'"
    }


    @SuppressWarnings([ 'UseCollectMany', 'UnnecessaryObjectReferences', 'AbcMetric' ])
    @Requires({ ! ext.updated })
    private void updateExtension()
    {
        ext.checks           = updateChecks( ext.checks, ext.port )
        ext.scriptPath       = ext.scriptPath ?: ( ext.knownScriptPaths ?: [] ).find { new File( projectDir, it ).file }
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
        ext.env.PORT     = ext.port

        // https://wiki.jenkins-ci.org/display/JENKINS/Spawning+processes+from+build
        if ( systemEnv.JENKINS_URL != null ){ ext.env.BUILD_ID = 'JenkinsLetMeSpawn' }

        addRedis()
        addMongo()
    }
}
