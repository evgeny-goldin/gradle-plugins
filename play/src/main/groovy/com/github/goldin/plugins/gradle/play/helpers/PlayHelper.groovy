package com.github.goldin.plugins.gradle.play.helpers

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import com.github.goldin.plugins.gradle.play.PlayExtension
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
    PlayHelper ( Project project, BaseTask task, PlayExtension ext ){ super( project, task, ext )}


    /**
     * Retrieves base part of the shell script to be used by various tasks.
     */
    @Requires({ operationTitle })
    @Ensures ({ result })
    String baseScript ( String operationTitle )
    {

        """
        |echo $LOG_DELIMITER
        |echo "Executing $Q$operationTitle$Q ${ operationTitle == this.name ? 'task' : 'step' } in $Q`pwd`$Q"
        |echo "Running   $SCRIPT_LOCATION"
        |echo $LOG_DELIMITER
        |
        """.stripMargin().toString().trim()
    }

}
