package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Starts Node.js application.
 */
class NodeStartTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        bashExec( startScript(), scriptFile( START_SCRIPT ), true, ext.generateOnly )
    }


    @Requires({ ext.startCommands || ext.scriptPath })
    @Ensures({ result })
    private String startScript()
    {
        String foreverCommand = ''

        if ( ext.isCoffee )
        {
            final  coffee  = project.file( COFFEE_EXECUTABLE )
            assert coffee.file, "[$coffee] executable is not available"
            foreverCommand = "\"$COFFEE_EXECUTABLE\""
        }

        final List<String> startCommands =
            ext.startCommands ?:
            [ "forever start --pidFile \"${ project.name }.pid\" $foreverCommand \"$ext.scriptPath\"" ]

        """
        |${ baseBashScript() }
        |export BUILD_ID=JenkinsLetMeSpawn
        |
        |${ startCommands*.trim().join( '\n|' )}""".stripMargin()
    }
}
