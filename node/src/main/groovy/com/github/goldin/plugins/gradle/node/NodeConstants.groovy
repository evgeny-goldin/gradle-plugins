package com.github.goldin.plugins.gradle.node

import java.util.regex.Pattern


/**
 * Various constants
 */
@SuppressWarnings([ 'PropertyName' ])
class NodeConstants
{
    final static String SETUP_SCRIPT      = 'setup.sh'
    final static String TEST_SCRIPT       = 'test.sh'
    final static String START_SCRIPT      = 'start.sh'

    final static String NODE_EXTENSION    = 'node'
    final static String CLEAN_TASK        = 'clean'
    final static String NODE_CLEAN_TASK   = 'nodeClean'
    final static String NODE_SETUP_TASK   = 'setup'
    final static String TEST_TASK         = 'test'
    final static String NODE_TEST_TASK    = 'nodeTest'
    final static String NODE_START_TASK   = 'start'

    final static String NODE_MODULES_DIR  = 'node_modules'
    final static String NODE_MODULES_BIN  = "$NODE_MODULES_DIR/.bin"

    final static Pattern AttributePattern = Pattern.compile( /(\w+)='(.*?[^|])'/ )
    final static Pattern NumberPattern    = Pattern.compile( /^\d+$/ )
}
