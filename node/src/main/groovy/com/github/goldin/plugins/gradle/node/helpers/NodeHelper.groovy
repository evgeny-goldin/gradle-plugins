package com.github.goldin.plugins.gradle.node.helpers

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import static com.github.goldin.plugins.gradle.common.node.NodeConstants.*
import org.gradle.api.Project
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import com.github.goldin.plugins.gradle.node.NodeExtension
import com.github.goldin.plugins.gradle.node.tasks.NodeBaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


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
    NodeHelper ( Project project, NodeBaseTask task, NodeExtension ext ){ super( project, task, ext )}


    /**
     * Retrieves PID file path.
     */
    @Ensures ({ result })
    File pidFile(){ new File( foreverHome(), "pids/${ pidFileName( ext.port ) }" )}


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
    File foreverHome() { home( '.forever' ) }


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


    /**
     * Retrieves commands to be used for killing the project's running processes.
     * @return commands to be used for killing the project's running processes
     */
    @Requires({ ext.scriptPath })
    @Ensures ({ result })
    String killProcesses()
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
    private List<Map<String, ?>> readConfigs()
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
     * Retrieves base part of the shell script to be used by various tasks.
     */
    @Requires({ operationTitle })
    @Ensures ({ result })
    String baseScript ( String operationTitle = this.task.name )
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
        |echo "Running   ${ ext.publicIp ? ' ' : '' }$Q$operationTitle$Q ${ operationTitle == this.name ? 'task' : 'step' } in $Q`pwd`$Q"
        |echo "Executing ${ ext.publicIp ? ' ' : '' }$SCRIPT_LOCATION"
        |${ ext.env.keySet().collect { "echo \"\\\$${ it.padRight( envPadSize )} = \$$it\"" }.join( '\n' ) }
        |echo $LOG_DELIMITER
        |
        """.stripMargin().toString().trim()
    }


    @Requires({ lists  != null })
    @Ensures ({ result != null })
    List<String> add( List<String> ... lists )
    {
        ( List<String> ) lists.inject( [] ){ List<String> sum, List<String> l -> sum + ( l ?: [] ) }
    }
}
