package com.github.goldin.plugins.gradle.node.tasks


import static com.github.goldin.plugins.gradle.node.NodeConstants.*

/**
 * Displays a help message with all tasks available.
 */
class HelpTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        System.out.println( "Gradle Node.js plugin. The following tasks are available:" )
        System.out.println( "- gradle $HELP_TASK:  displays this help message" )
        System.out.println( "- gradle $CLEAN_TASK: deletes '${ scriptsFolder.name }' folder where all *.sh scripts are created" )
        System.out.println( "- gradle $CLEAN_MODULES: deletes '$NODE_MODULES_DIR'" )
        System.out.println( "- gradle $SETUP_TASK: installs Node.js and all application dependencies" )
        System.out.println( "- gradle $TEST_TASK: runs application unit tests" )
        System.out.println( "- gradle $START_TASK: starts an application with 'forever'" )
        System.out.println( "- gradle $RESTART_ALL_TASK: restarts all currently running applications" )
        System.out.println( "- gradle $CHECK_STARTED_TASK: checks that application started is up and running" )
        System.out.println( "- gradle $STOP_TASK: stops an application" )
        System.out.println( "- gradle $STOP_ALL_TASK: stops all currently running applications" )
        System.out.println( "- gradle $CHECK_STOPPED_TASK: checks that application stopped isn't accepting connections" )
    }
}
