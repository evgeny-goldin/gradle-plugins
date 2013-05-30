package com.github.goldin.plugins.gradle.node
/**
 * Various constants
 */
@SuppressWarnings([ 'PropertyName' ])
class NodeConstants
{
    final static String REMOVE_COLOR_CODES     = "cat -v | sed 's/\\^\\[\\[[0-9;]*[m|K]//g'" // sed-only solution didn't work on Mac :(
    final static String LOG_DELIMITER          = '-----------------------------------------------'
    final static String Q                      = '"\\""' // Shell double quote (")
    final static String PACKAGE_JSON           = 'package.json'

    final static String NODE_VERSION_URL       = 'http://nodejs.org/'
    final static String NVM_GIT_REPO           = 'git://github.com/creationix/nvm.git'
    // https://github.com/creationix/nvm/commits/master
    final static String NVM_COMMIT             = '66633de1a9e8f5bc16fb2126e9d4f7d0abe07af0'


    final static String SCRIPT_LOCATION        = '[script-location]'
    final static String NODE_EXTENSION         = 'node'
    final static String HELP_TASK              = 'help'
    final static String CLEAN_TASK             = 'clean'
    final static String CLEAN_MODULES          = 'cleanModules'
    final static String SETUP_TASK             = 'setup'

    final static String RUN_TASK               = 'run'
    final static String TEST_TASK              = 'test'

    final static String START_TASK             = 'start'
    final static String LIST_TASK              = 'list'
    final static String RESTART_ALL_TASK       = 'restartall'
    final static String CHECK_STARTED_TASK     = 'checkStarted'

    final static String STOP_TASK              = 'stop'
    final static String STOP_ALL_TASK          = 'stopall'
    final static String CHECK_STOPPED_TASK     = 'checkStopped'

    final static String NODE_MODULES_DIR       = './node_modules'
    final static String MODULES_BIN_DIR        = "$NODE_MODULES_DIR/.bin"
    final static String COFFEE_EXECUTABLE      = "$MODULES_BIN_DIR/coffee"
    final static String FOREVER_EXECUTABLE     = "$MODULES_BIN_DIR/forever"
}
