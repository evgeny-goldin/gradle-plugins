package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.common.node.NodeConstants.*
import org.gcontracts.annotations.Ensures


/**
 * Displays a status of Node.js applications.
 */
class ListTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        shellExec( listProcesses(), baseScript(), scriptFileForTask(), false, false )
    }


    /**
     * Retrieves script commands for listing currently running Node.js processes.
     */
    @Ensures ({ result })
    private String listProcesses()
    {
        """
        |echo ${ forever() } list
        |echo
        |${ forever() } list ${ ext.removeColor ? '--plain' : '--colors' }${ ext.removeColorCodes }
        |echo $LOG_DELIMITER
        |echo \"ps -Af | grep ${Q}bin/node${Q} | grep -v grep\"
        |echo
        |ps -Af | grep node | grep -v grep
        |echo $LOG_DELIMITER
        """.stripMargin().toString().trim()
    }
}
