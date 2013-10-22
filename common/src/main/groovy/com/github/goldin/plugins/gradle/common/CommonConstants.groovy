package com.github.goldin.plugins.gradle.common

@SuppressWarnings([ 'GroovyConstantNamingConvention' ])
class CommonConstants
{
    static final String Q                  = '"\\""' // Shell double quote (")
    static final String SCRIPT_LOCATION    = '[script-location]'
    final static String LOG_DELIMITER      = '-----------------------------------------------'
    final static String REMOVE_COLOR_CODES = "cat -v | sed 's/\\^\\[\\[[0-9;]*[m|K]//g' | sed s/\\\\^M\\\\^\\\\[M//"
    final static String DOT_JS             = '.js'
    final static String DOT_COFFEE         = '.coffee'
    final static String DOT_CSS            = '.css'
    final static String DOT_LESS           = '.less'

    final static String HELP_TASK          = 'help'
    final static String CLEAN_TASK         = 'clean'
    final static String SETUP_TASK         = 'setup'
    final static String RUN_TASK           = 'run'
    final static String TEST_TASK          = 'test'
    final static String START_TASK         = 'start'
    final static String CHECK_STARTED_TASK = 'checkStarted'
    final static String STOP_TASK          = 'stop'
    final static String CHECK_STOPPED_TASK = 'checkStopped'
}
