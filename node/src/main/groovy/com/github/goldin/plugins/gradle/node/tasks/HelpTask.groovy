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
- gradle $HELP_TASK         : Displays this help message
- gradle $CLEAN_TASK        : Deletes '${ buildDir().name }' folder where all *.sh scripts are created
- gradle $CLEAN_MODULES : Deletes '$NODE_MODULES_DIR' folder
- gradle $SETUP_TASK        : Installs Node.js and all application dependencies, runs automatically
- gradle $RUN_TASK          : Runs commands specified when no application needs to be started or stopped
- gradle $TEST_TASK         : Runs application unit tests
- gradle $START_TASK        : Starts an application with 'forever'
- gradle $RESTART_ALL_TASK   : Restarts all currently running applications
- gradle $CHECK_STARTED_TASK : Checks that application has started and is running correctly, runs automatically after '$START_TASK' and '$RESTART_ALL_TASK'
- gradle $STOP_TASK         : Stops an application, runs automatically before '$START_TASK' and when '$CHECK_STARTED_TASK' fails
- gradle $STOP_ALL_TASK      : Stops all currently running applications
- gradle $CHECK_STOPPED_TASK : Checks that application has stopped and is not accepting connections any more, runs automatically after '$STOP_TASK' and '$STOP_ALL_TASK'
- gradle $LIST_TASK         : Lists all currently running Node.js applications, runs automatically after '$START_TASK', '$RESTART_ALL_TASK', '$STOP_TASK', and '$STOP_ALL_TASK'
""" )
    }
}
