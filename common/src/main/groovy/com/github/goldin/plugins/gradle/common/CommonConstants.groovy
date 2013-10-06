package com.github.goldin.plugins.gradle.common

@SuppressWarnings([ 'GroovyConstantNamingConvention' ])
class CommonConstants
{
    static final String Q                  = '"\\""' // Shell double quote (")
    static final String SCRIPT_LOCATION    = '[script-location]'
    final static String LOG_DELIMITER      = '-----------------------------------------------'
    final static String REMOVE_COLOR_CODES = "cat -v | sed 's/\\^\\[\\[[0-9;]*[m|K]//g' | sed s/\\\\^M\\\\^\\\\[M//"
}
