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
        System.out.println( "Hi, I'm Gradle Node.js plugin." )
        System.out.println( "You can run me as follows:" )
        System.out.println( "- gradle $HELP_TASK         : displays this help message" )
        System.out.println( "- gradle $CLEAN_TASK        : deletes '${ buildDir().name }' folder where all *.sh scripts are created" )
        System.out.println( "- gradle $CLEAN_MODULES : deletes '$NODE_MODULES_DIR' folder" )
        System.out.println( "- gradle $SETUP_TASK        : installs Node.js and all application dependencies, " +
                            "runs automatically" )
        System.out.println( "- gradle $RUN_TASK          : only runs commands specified, does nothing else" )
        System.out.println( "- gradle $TEST_TASK         : runs application unit tests" )
        System.out.println( "- gradle $START_TASK        : starts an application with 'forever'" )
        System.out.println( "- gradle $RESTART_ALL_TASK   : restarts all currently running applications" )
        System.out.println( "- gradle $CHECK_STARTED_TASK : checks that application started is up and running, " +
                            "runs automatically after '$START_TASK' and '$RESTART_ALL_TASK'" )
        System.out.println( "- gradle $STOP_TASK         : stops an application, " +
                            "runs automatically before '$START_TASK' and when '$CHECK_STARTED_TASK' fails" )
        System.out.println( "- gradle $STOP_ALL_TASK      : stops all currently running applications" )
        System.out.println( "- gradle $CHECK_STOPPED_TASK : checks that application stopped is not accepting connections any more, " +
                            "runs automatically after '$STOP_TASK' and '$STOP_ALL_TASK'" )
    }
}
