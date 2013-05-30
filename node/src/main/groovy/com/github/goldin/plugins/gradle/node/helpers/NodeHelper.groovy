package com.github.goldin.plugins.gradle.node.helpers

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gradle.api.Project
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.tasks.NodeBaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.logging.LogLevel


/**
 * Various helper methods for the Node plugin tasks.
 */
class NodeHelper extends BaseHelper<NodeExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    NodeHelper() { super( null, null, null )}


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    NodeHelper ( Project project, BaseTask task, NodeExtension ext ){ super( project, task, ext )}


    @Ensures({ result })
    File buildDir (){ project.buildDir }

    /**
     * Retrieves PID file path.
     */
    @Ensures ({ result })
    File pidFile(){ new File( foreverHome(), "pids/${ pidFileName( ext.portNumber ) }" )}


    /**
     * Retrieves .pid file name to use when application is started and stopped.
     */
    @Requires({ port > 0 })
    @Ensures ({ result   })
    String pidFileName( int port ){ ext.pidFileName ?: "${ projectName }-${ port }.pid" }


    /**
     * Retrieves 'forever' home path for storing logs and PID files.
     */
    @Ensures ({ result })
    File foreverHome() { new File( "${ systemProperties[ 'user.home' ]}/.forever" )}


    /**
     * Retrieves 'forever' executable path.
     */
    @Ensures ({ result })
    String forever()
    {
        checkFile( FOREVER_EXECUTABLE ).canonicalPath
    }


    /**
     * Retrieves latest Node.js version
     */
    @Ensures({ result })
    String latestNodeVersion(){ latestNodeVersion( httpRequest( NODE_VERSION_URL ).asString()) }


    /**
     * Retrieves latest Node.js version reading the content provided.
     */
    @Requires({ content })
    @Ensures ({ result  })
    String latestNodeVersion( String content )
    {
        content.find( ~/<p>Current Version: (.+?)<\/p>/ ){ it[ 1 ] }
    }


    @Requires({ scriptName })
    @Ensures ({ result })
    File scriptFileForTask ( String scriptName = this.name, boolean isBefore = false, boolean isAfter = false )
    {
        final fileName = ( isBefore ?  'before-' :
                           isAfter  ?  'after-'  :
                                       '' ) + "${ scriptName }.sh"

        new File( buildDir(), fileName )
    }


    @Requires({ taskName })
    void runTask( String taskName )
    {
        log{ "Running task '$taskName'" }
        final t = ( NodeBaseTask ) project.tasks[ taskName ]

        t.generalHelper = generalHelper
        t.ioHelper      = ioHelper
        t.jsonHelper    = jsonHelper
        t.matcherHelper = matcherHelper
        t.nodeHelper    = this
        t.cacheHelper   = (( NodeBaseTask ) task ).cacheHelper
        t.configHelper  = (( NodeBaseTask ) task ).configHelper

        t.taskAction()
    }


    /**
     * Retrieves commands to be used for killing the project's running processes.
     * @return commands to be used for killing the project's running processes
     */
    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    String killProcesses ()
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
    String commandsScript ( List<String> commands )
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
        final result = []

        for ( configMap in ( ext.configs ?: [] ))
        {
            configMap.each {
                String configPath, Object configValue ->
                result << ( configValue instanceof File ? new ConfigHelper( this.project, this.task, this.ext ).readConfigFile(( File ) configValue ) :
                            configValue instanceof Map  ? (( Map ) configValue ) :
                                                          [:] )
            }
        }

        result
    }


    /**
     * Executes the script specified as shell command.
     *
     * @param scriptContent  content to run as shell script
     * @param scriptFile     script file to create
     * @param watchExitCodes whether script exit codes need to be monitored (by adding set -e/set -o pipefail)
     * @param addBaseScript  whether a base script should be added (sets up environment variables)
     * @param failOnError    whether execution should fail if shell execution results in non-zero value
     * @param useGradleExec  whether Gradle (true) or Ant (false) exec is used
     * @param operationTitle title to display before the operation starts (if addBaseScript is true)
     * @param logLevel       log level to use for shell output
     *
     * @return shell output
     */
    @Requires({ scriptContent && scriptFile })
    @Ensures ({ result != null })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter', 'GroovyMethodParameterCount' ])
    String shellExec ( String   scriptContent,
                       File     scriptFile     = scriptFileForTask(),
                       boolean  watchExitCodes = true,
                       boolean  addBaseScript  = true,
                       boolean  failOnError    = true,
                       boolean  useGradleExec  = true,
                       String   operationTitle = this.name,
                       LogLevel logLevel       = LogLevel.INFO )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        delete( scriptFile )

        scriptContent = ( ext.transformers ?: [] ).inject(
        """#!${ ext.shell }
        |
        |${ watchExitCodes ? 'set -e'          : '' }
        |${ watchExitCodes ? 'set -o pipefail' : '' }
        |${ ext.verbose    ? 'set -x'          : '' }
        |
        |echo "cd $Q${ projectDir.canonicalPath }$Q"
        |cd "${ projectDir.canonicalPath }"
        |
        |${ addBaseScript ? baseScript( operationTitle ) : '' }
        |
        |${ scriptContent }
        """.stripMargin().toString().trim().
            replace( SCRIPT_LOCATION, "${Q}file:${ scriptFile.canonicalPath }${Q}" )) {
            String script, Closure transformer ->
            transformer( script, scriptFile, this ) ?: script
        }

        write( scriptFile, scriptContent.trim())

        log( LogLevel.INFO ){ "Shell script created at [$scriptFile.canonicalPath], size [${ scriptFile.length() }] bytes" }

        exec( 'chmod', [ '+x', scriptFile.canonicalPath ] )
        exec ( ext.shell, [ scriptFile.canonicalPath ], projectDir, failOnError, useGradleExec, logLevel )
    }


    /**
     * Retrieves base part of the shell script to be used by various tasks.
     */
    @Requires({ operationTitle })
    @Ensures ({ result })
    String baseScript ( String operationTitle )
    {
        assert ( ext.env && ( ! ext.env.values().any { it == null } ))

        ext.env.PATH     = "${ checkDirectory( MODULES_BIN_DIR ).canonicalPath }:\$PATH"
        final envPadSize = ext.env.keySet()*.length().max()

        """
        |${ ext.env.collect { String variable, Object value -> "export $variable=$value" }.join( '\n' )}
        |
        |. "\$HOME/.nvm/nvm.sh"
        |nvm use $ext.nodeVersion
        |echo Now using forever `${ forever() } --version`
        |
        |echo $LOG_DELIMITER${ ext.publicIp ? "\n|echo \"Running on $Q${ ext.publicIp }$Q\"" : '' }
        |echo "Executing ${ ext.publicIp ? ' ' : '' }$Q$operationTitle$Q ${ operationTitle == this.name ? 'task' : 'step' } in $Q`pwd`$Q"
        |echo "Running   ${ ext.publicIp ? ' ' : '' }$SCRIPT_LOCATION"
        |${ ext.env.keySet().collect { "echo \"\\\$${ it.padRight( envPadSize )} = \$$it\"" }.join( '\n' ) }
        |echo $LOG_DELIMITER
        |
        """.stripMargin().toString().trim()
    }


    @Requires({ lists  != null })
    @Ensures ({ result != null })
    List<String> add( List<String> ... lists )
    {
        lists.inject( [] ){ List<String> sum, List<String> l -> sum + ( l ?: [] ) }
    }
}
