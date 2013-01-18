package com.github.goldin.plugins.gradle.node

import java.util.regex.Pattern


/**
 * Various constants
 */
@SuppressWarnings([ 'PropertyName' ])
class NodeConstants
{
    final static String NODE_VERSION_URL    = 'http://nodejs.org/'
    final static String NVM_GIT_REPO        = 'git://github.com/creationix/nvm.git'

    final static String NODE_EXTENSION      = 'node'
    final static String CLEAN_TASK          = 'clean'
    final static String CLEAN_MODULES       = 'cleanModules'
    final static String SETUP_TASK          = 'setup'
    final static String TEST_TASK           = 'test'
    final static String STOP_TASK           = 'stop'
    final static String START_TASK          = 'start'
    final static String CHECK_STARTED_TASK  = 'checkStarted'
    final static String CHECK_STOPPED_TASK  = 'checkStopped'

    final static String SETUP_SCRIPT        = "${ SETUP_TASK }.sh"
    final static String TEST_SCRIPT         = "${ TEST_TASK }.sh"
    final static String BEFORE_TEST_SCRIPT  = "before-${ TEST_TASK }.sh"
    final static String AFTER_TEST_SCRIPT   = "after-${ TEST_TASK }.sh"
    final static String START_SCRIPT        = "${ START_TASK }.sh"
    final static String STOP_SCRIPT         = "${ STOP_TASK }.sh"
    final static String BEFORE_START_SCRIPT = "before-${ START_TASK }.sh"
    final static String AFTER_STOP_SCRIPT   = "after-${ STOP_TASK }.sh"

    final static String NODE_MODULES_DIR  = 'node_modules'
    final static String MODULES_BIN_DIR   = "$NODE_MODULES_DIR/.bin"
    final static String COFFEE_EXECUTABLE = "$MODULES_BIN_DIR/coffee"

    final static Pattern AttributePattern      = ~/(\w+)='(.*?[^|])'/
    final static Pattern EmptyAttributePattern = ~/(\w+)='()'/
    final static Pattern NumberPattern         = ~/^\d+$/
    final static Pattern KillPattern           = ~/<kill (.+?)>/
}
