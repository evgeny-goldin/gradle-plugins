package com.github.goldin.plugins.gradle.common.helpers

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.extensions.BaseShellExtension
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel


class ShellHelper extends BaseHelper<BaseShellExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    ShellHelper ( Project project, BaseTask task, BaseShellExtension ext ) { super( project, task, ext )}


    @Requires({ scriptName })
    @Ensures ({ result })
    File scriptFileForTask ( String scriptName = this.name, boolean isBefore = false, boolean isAfter = false )
    {
        final fileName = ( isBefore ?  'before-' :
                           isAfter  ?  'after-'  :
                                       '' ) + "${ scriptName }.sh"

        new File( buildDir(), fileName )
    }


    /**
     * Executes the script specified as shell command.
     *
     * @param scriptContent  content to run as shell script
     * @param baseScript     script to start a new script with
     * @param scriptFile     script file to create
     * @param watchExitCodes whether script exit codes need to be monitored (by adding set -e/set -o pipefail)
     * @param failOnError    whether execution should fail if shell execution results in non-zero value
     * @param useGradleExec  whether Gradle (true) or Ant (false) exec is used
     * @param disconnect     whether command should be run in the background (only when useGradleExec is false)
     * @param logLevel       log level to use for shell output
     *
     * @return shell output
     */
    @Requires({ scriptContent && scriptFile })
    @Ensures ({ result != null })
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter', 'GroovyMethodParameterCount' ])
    String shellExec ( String        scriptContent,
                       String        baseScript     = '',
                       File          scriptFile     = scriptFileForTask(),
                       boolean       watchExitCodes = true,
                       boolean       failOnError    = true,
                       boolean       useGradleExec  = true,
                       boolean       disconnect     = false,
                       LogLevel      logLevel       = LogLevel.INFO )
    {
        assert scriptFile.parentFile.with { directory  || project.mkdir ( delegate ) }, "Failed to create [$scriptFile.parentFile.canonicalPath]"
        assert ( ! disconnect ) || ( ! useGradleExec ), "Both 'disconnect' and 'useGradleExec' can't be true"
        assert ( ! disconnect ) || ( ! failOnError ),   "Both 'disconnect' and 'failOnError' can't be true"

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
        |${ baseScript ?: '' }
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
        exec( ext.shell, [ scriptFile.canonicalPath ], projectDir, failOnError, useGradleExec, disconnect, logLevel )
    }
}
