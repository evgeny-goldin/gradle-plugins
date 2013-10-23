package com.github.goldin.plugins.gradle.common.node


/**
 * Various Node.js constants
 */
@SuppressWarnings([ 'PropertyName' ])
class NodeConstants
{
    final static String PACKAGE_JSON           = 'package.json'

    final static String NODE_VERSION_URL       = 'http://nodejs.org/'
    final static String NVM_GIT_REPO           = 'https://github.com/creationix/nvm.git'
    // https://github.com/creationix/nvm/commits/master
    final static String NVM_COMMIT             = 'd3f04c793e19a87cfde4cd0b8a3a299facbbcc24'

    final static String NODE_EXTENSION         = 'node'
    final static String CLEAN_MODULES          = 'cleanModules'
    final static String LIST_TASK              = 'list'
    final static String RESTART_ALL_TASK       = 'restartall'
    final static String STOP_ALL_TASK          = 'stopall'

    final static String NODE_MODULES_DIR       = 'node_modules'
    final static String MODULES_BIN_DIR        = "$NODE_MODULES_DIR/.bin"
    final static String COFFEE_EXECUTABLE      = "$MODULES_BIN_DIR/coffee"
    final static String FOREVER_EXECUTABLE     = "$MODULES_BIN_DIR/forever"
    final static String GRUNT_EXECUTABLE       = "$MODULES_BIN_DIR/grunt"
}
