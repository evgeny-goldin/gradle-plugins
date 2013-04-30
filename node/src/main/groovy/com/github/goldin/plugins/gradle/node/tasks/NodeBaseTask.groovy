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
     */
    @Ensures ({ result })
    String forever()
    {
        final  foreverExecutable = project.file( FOREVER_EXECUTABLE ).canonicalFile
        assert foreverExecutable.file
        FOREVER_EXECUTABLE
    }


    @Override
    void verifyUpdateExtension ( String description )
    {
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


    private void updateExtension()
    {
        ext.checkUrl   = ext.checkUrl.startsWith( 'http' ) ?
            ext.checkUrl :
            "http://127.0.0.1:${ ext.portNumber }" + ( ext.checkUrl ? "/${ ext.checkUrl.replaceFirst( '^/', '' ) }"  : '' )
        assert ext.checkUrl

        ext.scriptPath = ext.scriptPath ?: ( ext.knownScriptPaths ?: [] ).find { new File( projectDir, it ).file }
        assert ( ext.scriptPath || ( ! requiresScriptPath()) || ( ext.run )), \
               "Couldn't find an application script to run! Specify 'scriptPath' in $description or use " +
               "'${ ( ext.knownScriptPaths ?: [] ).join( "', '" ) }'"

        ext.nodeVersion      = ( ext.nodeVersion == 'latest' ) ? nodeHelper.latestNodeVersion() : ext.nodeVersion
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


    /**
     * Adds Redis before/after steps, if needed.
     */
    private void addRedis()
    {
        final addRedis  = (( ext.redisPort > 0 ) || ext.redisPortConfigKey || ext.redisCommandLine )
        if (  addRedis )
        {
            final redisPort    = ( ext.redisPort > 0      ) ? ext.redisPort.toString() :
                                 ( ext.redisPortConfigKey ) ? '${ config.' + ext.redisPortConfigKey + ' }' :
                                                              '6379'

            final redisRunning = """ "`redis-cli -p $redisPort ping 2>&1`" = "PONG"  """.trim()
            final isStartRedis = (( ext.redisStartInProduction ) || ( ext.NODE_ENV != 'production' ))
            final isStopRedis  = (( ext.redisStopInProduction  ) || ( ext.NODE_ENV != 'production' ))
            final getScript    = { String scriptName -> resourceText( scriptName,
            [
                redisPort        : redisPort,
                redisRunning     : redisRunning,
                redisCommandLine : ext.redisCommandLine ?: '',
                sleep            : ext.redisWait as String
            ])}
            ext.before = ( isStartRedis ? getScript( 'redis-start.sh' ).readLines() : [] ) + ( ext.before ?: [] )
            ext.after  = ( isStopRedis  ? getScript( 'redis-stop.sh'  ).readLines() : [] ) + ( ext.after  ?: [] )
        }
    }


    /**
     * Adds MongoDB before/after steps, if needed.
     */
    private void addMongo()
    {
        final addMongo = (( ext.mongoPort > 0 ) || ext.mongoPortConfigKey || ext.mongoCommandLine || ext.mongoLogpath || ext.mongoDBPath )
        if (  addMongo )
        {
            final mongoPort    = ( ext.mongoPort > 0      ) ? ext.mongoPort.toString() :
                                 ( ext.mongoPortConfigKey ) ? '${ config.' + ext.mongoPortConfigKey + ' }' :
                                                              '27017'

            final mongoEval    = """ "`mongo --eval ${Q}db${Q} --port $mongoPort 2>&1 | tail -1`" """.trim()
            final mongoRunning = """ ! $mongoEval =~ (command not found|connect failed|couldn\\'t connect to server) """.trim()
            final isStartMongo = (( ext.mongoStartInProduction ) || ( ext.NODE_ENV != 'production' ))
            final isStopMongo  = (( ext.mongoStopInProduction  ) || ( ext.NODE_ENV != 'production' ))
            final getScript    = { String scriptName -> resourceText( scriptName,
            [
                mongoPort        : mongoPort,
                mongoRunning     : mongoRunning,
                mongoDBPath      : fullPath( ext.mongoDBPath,  '/data/db/'  ),
                mongoLogpath     : fullPath( ext.mongoLogpath, 'mongod.log' ),
                mongoCommandLine : ext.mongoCommandLine ?: '',
                sleep            : ext.mongoWait as String
            ])}

            ext.before = ( isStartMongo ? getScript( 'mongo-start.sh' ).readLines() : [] ) + ( ext.before ?: [] )
            ext.after  = ( isStopMongo  ? getScript( 'mongo-stop.sh'  ).readLines() : [] ) + ( ext.after  ?: [] )
        }
    }


    @Requires({ this.name })
    @Ensures ({ result })
    final File taskScriptFile ( boolean before = false, boolean after = false, String name = null )
    {
        final fileName = ( before ?  'before-' :
                           after  ?  'after-'  :
                                     '' ) + ( name ?: this.name ) + '.sh'

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
    final String pidFileName( int port ){ ext.pidFileName ?: "${ projectName }-${ port }.pid" }


    /**
     * Retrieves script commands for listing currently running Node.js processes.
     */
    @Ensures ({ result })
    final String listProcesses( boolean startWithDelimiterLine = true )
    {
        """
        |${ startWithDelimiterLine ? "echo $LOG_DELIMITER" : '' }
        |echo ${ forever() } list
        |echo
        |${ forever() } list ${ ext.removeColor ? '--plain' : '--colors' }${ ext.removeColorCodes }
        |echo $LOG_DELIMITER
        |echo \"ps -Af | grep node | grep -v grep\"
        |echo
        |ps -Af | grep node | grep -v grep
        |echo $LOG_DELIMITER
        """.stripMargin().toString().trim()
    }


    /**
     * Retrieves commands to be used for killing the project's running processes.
     * @return commands to be used for killing the project's running processes
     */
    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    final String killProcesses ()
    {
        final  killProcesses = "forever,${ projectDir.name }|${ ext.scriptPath },${ projectDir.name }"
        killProcesses.trim().tokenize( '|' )*.trim().grep().collect {
            String process ->

            final processGrepSteps = process.tokenize( ',' )*.replace( "'", "'\\''" ).collect { "grep '$it'" }.join( ' | ' )
            final listProcesses    = "ps -Af | $processGrepSteps | grep -v 'grep'"
            final pids             = "$listProcesses | awk '{print \$2}'"
            final killAll          = "$pids | while read pid; do echo \"kill \$pid\"; kill \$pid; done"
            final forceKillAll     = "$pids | while read pid; do echo \"kill -9 \$pid\"; kill -9 \$pid; done"
            final ifStillRunning   = "if [ \"`$pids`\" != \"\" ]; then"

            """
            |$ifStillRunning $killAll; fi
            |$ifStillRunning sleep 5; $forceKillAll; fi
            |$ifStillRunning echo 'Failed to kill process [$process]:'; $listProcesses; exit 1; fi
            """.stripMargin().toString().trim()

        }.join( '\n' )
    }


    /**
     * Generates a script containing the commands specified.
     *
     * @param commands commands to execute
     * @return script content generated
     */
    @Requires({ commands })
    @Ensures ({ result })
    final String commandsScript ( List<String> commands )
    {
        final script = commands.join( '\n' )

        if ( script.contains( '$' ))
        {
            if ( ext.configsResult == null ) { ext.configsResult = readConfigs() }
            assert ( ext.configsResult != null )
            final Map binding = [ configs : ext.configsResult ] + ( ext.configsResult ? [ config : ext.configsResult.head() ] : [:] )
            renderTemplate( script, binding )
        }
        else
        {
            script
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
     * @param scriptContent     content to run as bash script
     * @param scriptFile        script file to create
     * @param watchExitCodes    whether script exit codes need to be monitored (by adding set -e/set -o pipefail)
     * @param addBaseBashScript whether a base bash script should be added (sets up environment variables)
     * @param failOnError       whether execution should fail if bash execution results in non-zero value
     * @param useGradleExec     whether Gradle (true) or Ant (false) exec is used
     * @param operationTitle    title to display before the operation starts (if addBaseBashScript is true)
     * @param logLevel          log level to use for bash output
     *
     * @return bash output or empty String if bash was generated but not executed or
     */
    @Requires({ scriptContent && scriptFile })
    @Ensures ({ result != null })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter', 'GroovyMethodParameterCount' ])
    final String bashExec( String   scriptContent,
                           File     scriptFile        = taskScriptFile(),
                           boolean  watchExitCodes    = true,
                           boolean  addBaseBashScript = true,
                           boolean  failOnError       = true,
                           boolean  useGradleExec     = true,
                           String   operationTitle    = this.name,
                           LogLevel logLevel          = LogLevel.INFO )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        delete( scriptFile )

        scriptContent = ( ext.transformers ?: [] ).inject(
        """#!${ ext.shell }
        |
        |${ watchExitCodes    ? 'set -e'          : '' }
        |${ watchExitCodes    ? 'set -o pipefail' : '' }
        |
        |echo "cd $Q${ projectDir.canonicalPath }$Q"
        |cd "${ projectDir.canonicalPath }"
        |
        |${ addBaseBashScript ? baseBashScript( operationTitle ) : '' }
        |
        |${ scriptContent }
        """.stripMargin().toString().trim().
            replace( SCRIPT_LOCATION, "${Q}file:${ scriptFile.canonicalPath }${Q}" )) {
            String script, Closure transformer ->
            transformer( script, scriptFile, this ) ?: script
        }

        write( scriptFile, scriptContent.trim())

        log( LogLevel.INFO ){ "Bash script created at [$scriptFile.canonicalPath], size [${ scriptFile.length() }] bytes" }

        if ( isLinux || isMac ) { exec( 'chmod', [ '+x', scriptFile.canonicalPath ]) }

        exec ( ext.shell, [ scriptFile.canonicalPath ], projectDir, failOnError, useGradleExec, logLevel )
    }


    /**
     * Retrieves base part of the bash script to be used by various tasks.
     */
    @Requires({ operationTitle })
    @Ensures ({ result })
    private String baseBashScript ( String operationTitle )
    {
        final  binFolder = project.file( MODULES_BIN_DIR ).canonicalFile
        assert binFolder.directory, "Directory [$binFolder.canonicalPath] is not available"

        final isJenkins    = System.getenv( 'JENKINS_URL' ) != null
        final envVariables = [ 'NODE_ENV', 'PORT', 'PATH' ] +
                             ( isJenkins ? [ 'BUILD_ID' ]   : [] ) +
                             ( ext.env   ? ext.env.keySet() : [] )
        final padSize      = envVariables*.length().max()

        """
        |export NODE_ENV=$ext.NODE_ENV
        |export PORT=$ext.portNumber
        |export PATH=$binFolder:\$PATH
        |${ isJenkins ? 'export BUILD_ID=JenkinsLetMeSpawn' : '' }
        |${ ext.env   ? ext.env.collect { String variable, Object value -> "export $variable=$value" }.join( '\n' ) : '' }
        |
        |. "\$HOME/.nvm/nvm.sh"
        |nvm use $ext.nodeVersion
        |
        |echo $LOG_DELIMITER
        |echo "Executing $Q$operationTitle$Q ${ operationTitle == this.name ? 'task' : 'step' } in $Q`pwd`$Q"
        |echo "Running   $SCRIPT_LOCATION"
        |${ envVariables.collect { "echo \"\\\$${ it.padRight( padSize )} = \$$it\"" }.join( '\n' ) }
        |echo $LOG_DELIMITER
        |
        """.stripMargin().toString().trim()
    }


    @Requires({ resourcePath && ( replacements != null ) })
    final resourceText( String resourcePath, Map<String, String> replacements = [:] )
    {
        getResourceText( resourcePath, replacements + [ shell : ext.shell ])
    }



    @Requires({ lists  != null })
    @Ensures ({ result != null })
    final List<String> add( List<String> ... lists )
    {
        lists.inject( [] ){ List<String> sum, List<String> l -> sum + ( l ?: [] ) }
    }
}
