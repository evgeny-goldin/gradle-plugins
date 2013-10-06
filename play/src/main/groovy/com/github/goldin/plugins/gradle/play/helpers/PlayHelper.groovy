package com.github.goldin.plugins.gradle.play.helpers

import static com.github.goldin.plugins.gradle.play.PlayConstants.*
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import com.github.goldin.plugins.gradle.play.PlayExtension
import com.github.goldin.plugins.gradle.play.tasks.PlayBaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Various helper methods for the Play plugin tasks.
 */
class PlayHelper extends BaseHelper<PlayExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    PlayHelper ( Project project, PlayBaseTask task, PlayExtension ext ){ super( project, task, ext )}


    /**
     * Retrieves base part of the shell script to be used by various tasks.
     */
    @Requires({ operationTitle })
    @Ensures ({ result })
    String baseScript ( String operationTitle = this.task.name )
    {
        assert ( ext.env && ( ! ext.env.values().any { it == null } ))
        final envPadSize = ext.env.keySet()*.length().max()

        """
        |${ ext.env.collect { String variable, Object value -> "export $variable=$value" }.join( '\n' )}
        |
        |echo $LOG_DELIMITER
        |echo "Running   $Q$operationTitle$Q${ operationTitle == this.name ? ' task' : '' } in $Q`pwd`$Q"
        |echo "Executing $SCRIPT_LOCATION"
        |${ ext.env.keySet().collect { "echo \"\\\$${ it.padRight( envPadSize )} = \$$it\"" }.join( '\n' ) }
        |echo $LOG_DELIMITER
        |
        """.stripMargin().toString().trim()
    }


    /**
     * Builds application's startup arguments.
     *
     * http://www.playframework.com/documentation/2.2.x/Production
     * http://www.playframework.com/documentation/2.2.x/ProductionConfiguration
     */
    @Ensures ({ result })
    String arguments()
    {
        final arguments = new StringBuilder()

        arguments << " -Dhttp.port='${ ext.port }'"
        arguments << " -Dhttp.address='${ ext.address }'"
        arguments << " -Dconfig.file='${ ext.config }'"
        arguments << " -Dpidfile.path='${RUNNING_PID}'"
        arguments << " ${ ext.arguments }"

        arguments.toString().trim()
    }
}
