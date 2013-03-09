package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.node.ConfigHelper
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.NodeHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.logging.LogLevel


/**
 * Base class for all Node tasks.
 */
abstract class NodeBaseTask extends BaseTask<NodeExtension>
{
    @Override
    Class extensionType (){ NodeExtension }


    final NodeHelper nodeHelper = new NodeHelper( logger )

    @Ensures({ result })
    final File buildDir (){ project.buildDir }


    /**
     * Determines if current task requires an existence of {@link NodeExtension#scriptPath}
     * @return true if current task requires an existence of {@link NodeExtension#scriptPath},
     *         false otherwise
     */
    protected boolean requiresScriptPath(){ false }


    /**
     * Retrieves 'forever' executable path.
     * @return 'forever' executable path
     */
    @Ensures ({ result })
    String forever()
    {
        final  foreverExecutable = project.file( FOREVER_EXECUTABLE ).canonicalFile
        assert foreverExecutable.file, \
               "[$foreverExecutable.canonicalPath] is not available, make sure \"forever\" dependency appears in \"package.json\" => \"devDependencies\""
        FOREVER_EXECUTABLE
    }


    @Override
    void verifyUpdateExtension ( String description )
    {
        assert ext.NODE_ENV,            "'NODE_ENV' should be defined in $description"
        assert ext.nodeVersion,         "'nodeVersion' should be defined in $description"
        assert ext.testCommand,         "'testCommand' should be defined in $description"
        assert ext.configsKeyDelimiter, "'configsKeyDelimiter' should be defined in $description"
        assert ext.portNumber   >  0,   "'portNumber' should be positive in $description"
        assert ext.checkWait    >= 0,   "'checkWait' should not be negative in $description"
        assert ext.checkTimeout >= 0,   "'checkTimeout' should not be negative in $description"
        assert ext.redisWait    >= 0,   "'redisWait' should not be negative in $description"
        assert ext.mongoWait    >= 0,   "'mongoWait' should not be negative in $description"
        assert ext.configsNewKeys,      "'configsNewKeys' should be defined in $description"

        ext.checkUrl   = ext.checkUrl   ?: "http://127.0.0.1:${ ext.portNumber }"
        ext.scriptPath = ext.scriptPath ?: ( new File( project.projectDir, 'server.js'     ).file ? 'server.js'     :
                                             new File( project.projectDir, 'server.coffee' ).file ? 'server.coffee' :
                                                                                                    null )
        assert ext.checkUrl
        assert ( ext.scriptPath || ( ! requiresScriptPath())), \
               "'scriptPath' should be defined in $description or use 'server.[js|coffee]' script to auto-discover it"

        ext.nodeVersion = ( ext.nodeVersion == 'latest' ) ? nodeHelper.latestNodeVersion() : ext.nodeVersion

        addRedis()
        addMongo()
    }


    /**
     * Adds Redis before/after steps, if needed.
     */
    private void addRedis()
    {
        final addRedis  = (( ! ext.redisAddedAlready ) && (( ext.redisPort > 0 ) || ext.redisPortConfigKey ))
        if (  addRedis )
        {
            final redisPort    = ( ext.redisPort > 0 ) ? ext.redisPort as String : '${ config.' + ext.redisPortConfigKey + ' }'
            final redisRunning = "\"`redis-cli -p $redisPort ping 2> /dev/null`\" = \"PONG\""
            final isStartRedis = (( ext.redisStartInProduction ) || ( ext.NODE_ENV != 'production' ))
            final isStopRedis  = (( ext.redisStopInProduction  ) || ( ext.NODE_ENV != 'production' ))
            final getScript    = { String scriptName -> getResourceText( scriptName ).
                                                        replace( '${redisPort}',        redisPort ).
                                                        replace( '${redisRunning}',     redisRunning ).
                                                        replace( '${redisCommandLine}', ext.redisCommandLine ?: '' ).
                                                        replace( '${sleep}',            ext.redisWait as String )}
            ext.before            = ( isStartRedis ? getScript( 'redis-start.sh' ).readLines() : [] ) + ( ext.before ?: [] )
            ext.after             = ( isStopRedis  ? getScript( 'redis-stop.sh'  ).readLines() : [] ) + ( ext.after  ?: [] )
            ext.redisAddedAlready = true
        }
    }


    /**
     * Adds MongoDB before/after steps, if needed.
     */
    private void addMongo()
    {
        final addMongo  = (( ! ext.mongoAddedAlready ) && (( ext.mongoPort > 0 ) || ext.mongoPortConfigKey ))
        if (  addMongo )
        {
            final mongoPort    = ( ext.mongoPort > 0 ) ? ext.mongoPort as String : '${ config.' + ext.mongoPortConfigKey + ' }'
            final mongoRunning = """ ! "`mongo --eval ${Q}db${Q} --port 27017 2> /dev/null | tail -1`" =~ "couldn't connect to server" """
            final isStartMongo = (( ext.mongoStartInProduction ) || ( ext.NODE_ENV != 'production' ))
            final isStopMongo  = (( ext.mongoStopInProduction  ) || ( ext.NODE_ENV != 'production' ))
            final getScript    = { String scriptName -> getResourceText( scriptName ).
                                                        replace( '${mongoPort}',        mongoPort ).
                                                        replace( '${mongoRunning}',     mongoRunning ).
                                                        replace( '${mongoCommandLine}', ext.mongoCommandLine ?: '' ).
                                                        replace( '${mongoLogpath}',     ext.mongoLogpath     ?: 'mongod.log' ).
                                                        replace( '${mongoDBPath}',      ext.mongoDBPath      ?: '/data/db/' ).
                                                        replace( '${sleep}',            ext.mongoWait as String )}

            ext.before            = ( isStartMongo ? getScript( 'mongo-start.sh' ).readLines() : [] ) + ( ext.before ?: [] )
            ext.after             = ( isStopMongo  ? getScript( 'mongo-stop.sh'  ).readLines() : [] ) + ( ext.after  ?: [] )
            ext.mongoAddedAlready = true
        }
    }


    @Requires({ this.name })
    @Ensures ({ result })
    final File taskScriptFile ( boolean before = false, boolean after = false )
    {
        final fileName = ( before ? 'before-' :
                           after  ? 'after-'  :
                                    '' ) + this.name + '.sh'

        new File( buildDir(), fileName )
    }


    @Requires({ taskName })
    final void runTask( String taskName )
    {
        log{ "Running task '$taskName'" }
        (( NodeBaseTask ) project.tasks[ taskName ] ).taskAction()
    }


    /**
     * Retrieves .pid file name to use when application is started and stopped.
     * @param port application port
     * @return .pid file name to use when application is started and stopped
     */
    @Requires({ port > 0 })
    @Ensures ({ result   })
    final String pidFileName( int port ){ "${ project.name }-${ port }.pid" }


    /**
     * Retrieves base part of the bash script to be used by various tasks.
     */
    @Requires({ operationTitle })
    @Ensures ({ result })
    final String baseBashScript ( String operationTitle = this.name )
    {
        final  binFolder = project.file( MODULES_BIN_DIR ).canonicalFile
        assert binFolder.directory, "Directory [$binFolder.canonicalPath] is not available"

        """
        |export NODE_ENV=$ext.NODE_ENV
        |export PORT=$ext.portNumber
        |export PATH=$binFolder:\$PATH
        |export BUILD_ID=JenkinsLetMeSpawn
        |
        |. "\$HOME/.nvm/nvm.sh"
        |nvm use $ext.nodeVersion
        |
        |echo ---------------------------------------------
        |echo "Executing $Q$operationTitle$Q ${ operationTitle == this.name ? 'task' : 'step' } in $Q`pwd`$Q"
        |echo "Running   [script-location]"
        |echo \"\\\$NODE_ENV = $Q$ext.NODE_ENV$Q\"
        |echo \"\\\$PORT     = $Q$ext.portNumber$Q\"
        |echo \"\\\$PATH     = $Q$binFolder:\$PATH$Q\"
        |echo ---------------------------------------------
        |
        """.stripMargin()
    }


    /**
     * Generates a script containing the commands specified.
     *
     * @param commands commands to execute
     * @param title    commands title
     * @return script content generated
     */
    @Requires({ commands && title })
    @Ensures ({ result })
    final String commandsScript ( List<String> commands, String title )
    {
        final script = commands.join( '\n' )

        if ( script.contains( '$' ))
        {
            if ( ext.configsResult == null ) { ext.configsResult = readConfigs() }
            assert ( ext.configsResult != null )

            final Map binding = [ configs : ext.configsResult ] + ( ext.configsResult ? [ config : ext.configsResult.head() ] : [:] )

            baseBashScript( title ) + '\n' + renderTemplate( script, binding )
        }
        else
        {
            baseBashScript( title ) + '\n' + script
        }
    }


    @Ensures ({ result != null })
    private List<Map<String, ?>> readConfigs ()
    {
        final result       = []
        final configHelper = new ConfigHelper( ext, this )

        for ( configMap in ( ext.configs ?: [] ))
        {
            configMap.each {
                String configPath, Object configValue ->
                result << ( configValue instanceof File ? configHelper.readConfigFile(( File ) configValue ) :
                            configValue instanceof Map  ? (( Map ) configValue ) :
                                                          [:] )
            }
        }

        result
    }


    /**
     * Executes the script specified as bash command.
     *
     * @param scriptContent  content to run as bash script
     * @param scriptFile     script file to create
     * @param watchExitCodes whether script exit codes need to be monitored (by adding set -e/set -o pipefail)
     * @param failOnError    whether execution should fail if bash execution results in non-zero value
     * @param useGradleExec  whether Gradle (true) or Ant (false) exec is used
     *
     * @return bash output or empty String if bash was generated but not executed or
     */
    @Requires({ scriptContent && scriptFile })
    @Ensures ({ result != null })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    final String bashExec( String  scriptContent,
                           File    scriptFile,
                           boolean watchExitCodes = true,
                           boolean failOnError    = true,
                           boolean useGradleExec  = true )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        delete( scriptFile )

        scriptContent = ( ext.transformers ?: [] ).inject(
        """#!/bin/bash
        |
        |${ watchExitCodes ? 'set -e'          : '' }
        |${ watchExitCodes ? 'set -o pipefail' : '' }
        |
        |${ scriptContent.readLines().join( '\n|' ) }
        |""".stripMargin().
             replace( '[script-location]', "${Q}file:${ scriptFile.canonicalPath }${Q}" )){
            String script, Closure transformer ->
            transformer( script, scriptFile, this ) ?: script
        }

        write( scriptFile, scriptContent )

        log( LogLevel.INFO ){ "Bash script created at [$scriptFile.canonicalPath], size [${ scriptFile.length() }] bytes" }

        if ( isLinux || isMac ) { exec( 'chmod', [ '+x', scriptFile.canonicalPath ]) }

        exec ( 'bash', [ scriptFile.canonicalPath ], project.projectDir, failOnError, useGradleExec )
    }


    /**
     * Retrieves commands to be used for killing the project's running processes.
     * @return commands to be used for killing the project's running processes
     */
    @Requires({ project.name && ext.scriptPath })
    @Ensures ({ result })
    final List<String> killCommands ()
    {
        final  killProcesses = "forever,${ project.name }|${ ext.scriptPath },${ project.name }"
        killProcesses.trim().tokenize( '|' )*.trim().grep().collect {
            String process ->

            final processGrepSteps = process.tokenize( ',' )*.replace( "'", "'\\''" ).collect { "grep '$it'" }.join( ' | ' )
            final listProcesses    = "ps -Af | $processGrepSteps | grep -v 'grep'"
            final pids             = "$listProcesses | awk '{print \$2}'"
            final killAll          = "$pids | while read pid; do echo \"kill \$pid\"; kill \$pid; done"
            final forceKillAll     = "$pids | while read pid; do echo \"kill -9 \$pid\"; kill -9 \$pid; done"
            final ifStillRunning   = "if [ \"`$pids`\" != \"\" ]; then"

            [ "$ifStillRunning $killAll; fi",
              "$ifStillRunning sleep 5; $forceKillAll; fi",
              "$ifStillRunning echo 'Failed to kill process [$process]:'; $listProcesses; exit 1; fi" ]
        }.flatten()
    }
}
