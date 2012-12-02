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
        startCheck()
    }


    @Ensures({ result })
    private String startScript()
    {
        """
        |${ baseBashScript() }
        |export BUILD_ID=JenkinsLetMeSpawn
        |
        |${ startCommands().join( '\n|' )}""".stripMargin()
    }


    @Requires({ ext.startCommands || ext.scriptPath })
    @Ensures({ result })
    private List<String> startCommands()
    {
        if ( ext.startCommands ) { return ext.startCommands }

        String foreverCommand = ''

        if ( ext.isCoffee )
        {
            file( COFFEE_EXECUTABLE ) // Validates existence
            foreverCommand = "\"$COFFEE_EXECUTABLE\""
        }

        [ "forever start --pidFile \"${ project.name }.pid\" $foreverCommand \"$ext.scriptPath\"" ]
    }


    private void startCheck()
    {
        if ( ext.startCheckUrl )
        {
            delay( ext.startCheckDelay )
            final response = ext.startCheckUrl.toURL().text

            assert (( ext.startCheckResponse == response ) || ( ! ext.startCheckResponse )), \
                   "Requesting [$ext.startCheckUrl] returned [$response] instead of expected [$ext.startCheckResponse]"

            log{ "Requesting [$ext.startCheckUrl] returned [$response]" }
        }
    }
}
