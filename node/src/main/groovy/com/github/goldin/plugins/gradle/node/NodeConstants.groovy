package com.github.goldin.plugins.gradle.node

import java.util.regex.Pattern


/**
 * Various constants
 */
@SuppressWarnings([ 'PropertyName' ])
class NodeConstants
{
    final static String SETUP_SCRIPT     = 'setup-node.sh'
    final static String TEST_SCRIPT      = 'run-test.sh'
    final static String NODE_TEST_TASK   = 'nodeTest'
    final static String NODE_MODULES_BIN = './node_modules/.bin'

    final static Pattern NameAttributePattern     = Pattern.compile( /name='(.+?)'/ )
    final static Pattern MessageAttributePattern  = Pattern.compile( /message='(.+?)'/ )
    final static Pattern DurationAttributePattern = Pattern.compile( /duration='(\d+?)'/ )
}
