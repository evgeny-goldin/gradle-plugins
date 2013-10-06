package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.CommonConstants


/**
 * Various constants
 */
@SuppressWarnings([ 'PropertyName' ])
class NodeConstants extends CommonConstants
{
    final static String PACKAGE_JSON           = 'package.json'

    final static String NODE_VERSION_URL       = 'http://nodejs.org/'
    final static String NVM_GIT_REPO           = 'https://github.com/creationix/nvm.git'
    // https://github.com/creationix/nvm/commits/master
    final static String NVM_COMMIT             = 'db3035c29b2d1aa12be2411a2cdaeb2ca0bd530d'

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

    final static String NODE_MODULES_DIR       = 'node_modules'
    final static String MODULES_BIN_DIR        = "$NODE_MODULES_DIR/.bin"
    final static String COFFEE_EXECUTABLE      = "$MODULES_BIN_DIR/coffee"
    final static String FOREVER_EXECUTABLE     = "$MODULES_BIN_DIR/forever"
}
