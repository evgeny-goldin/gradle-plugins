package com.github.goldin.plugins.gradle.node.tasks


import static com.github.goldin.plugins.gradle.node.NodeConstants.*

/**
 * Displays a help message with all tasks available.
 */
@SuppressWarnings([ 'SystemOutPrint', 'UnnecessaryGString' ])
class HelpTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        System.out.println( "Hello, I'm Gradle Node.js plugin." )
        System.out.println( "You can run me as follows:" )
        System.out.println( "- gradle $HELP_TASK        : displays this help message" )
        System.out.println( "- gradle $CLEAN_TASK       : deletes '${ scriptsFolder().name }' folder where all *.sh scripts are created" )
        System.out.println( "- gradle $CLEAN_MODULES: deletes '$NODE_MODULES_DIR' folder" )
        System.out.println( "- gradle $SETUP_TASK       : installs Node.js and all application dependencies" )
        System.out.println( "                       runs automatically" )
        System.out.println( "- gradle $TEST_TASK        : runs application unit tests" )
        System.out.println( "- gradle $START_TASK       : starts an application with 'forever'" )
        System.out.println( "- gradle $RESTART_ALL_TASK  : restarts all currently running applications" )
        System.out.println( "- gradle $CHECK_STARTED_TASK: checks that application started is up and running" )
        System.out.println( "                       runs automatically after start and restartall" )
        System.out.println( "- gradle $STOP_TASK        : stops an application" )
        System.out.println( "                       runs automatically before '$START_TASK' and if '$CHECK_STARTED_TASK' fails" )
        System.out.println( "- gradle $STOP_ALL_TASK     : stops all currently running applications" )
        System.out.println( "- gradle $CHECK_STOPPED_TASK: checks that application stopped is not accepting connections any more" )
        System.out.println( "                       runs automatically after stop and stopall" )
    }
}
