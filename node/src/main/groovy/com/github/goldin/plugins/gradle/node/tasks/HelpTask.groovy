package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*


/**
 * Displays a help message with all tasks available.
 */
@SuppressWarnings([ 'SystemOutPrint', 'UnnecessaryGString', 'LineLength' ])
class HelpTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        System.out.println( """
Hi, I'm Gradle Node.js plugin!
You can run me as follows:
- gradle $HELP_TASK         : displays this help message
- gradle $CLEAN_TASK        : deletes '${ buildDir().name }' folder where all *.sh scripts are created
- gradle $CLEAN_MODULES : deletes '$NODE_MODULES_DIR' folder
- gradle $SETUP_TASK        : installs Node.js and all application dependencies, runs automatically
- gradle $RUN_TASK          : runs commands specified without starting a Node.js application
- gradle $TEST_TASK         : runs application unit tests
- gradle $START_TASK        : starts an application with 'forever'
- gradle $RESTART_ALL_TASK   : restarts all currently running applications
- gradle $CHECK_STARTED_TASK : checks that application started is up and running runs automatically after '$START_TASK' and '$RESTART_ALL_TASK'
- gradle $STOP_TASK         : stops an application runs automatically before '$START_TASK' and when '$CHECK_STARTED_TASK' fails
- gradle $STOP_ALL_TASK      : stops all currently running applications
- gradle $CHECK_STOPPED_TASK : checks that application stopped is not accepting connections any more runs automatically after '$STOP_TASK' and '$STOP_ALL_TASK'
- gradle $LIST_TASK         : lists all currently running Node.js applications, runs automatically after '$START_TASK', '$RESTART_ALL_TASK', '$STOP_TASK', and '$STOP_ALL_TASK'
""" )
    }
}
