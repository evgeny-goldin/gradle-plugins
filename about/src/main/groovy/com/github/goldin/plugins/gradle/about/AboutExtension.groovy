package com.github.goldin.plugins.gradle.about

import java.util.regex.Pattern


class AboutExtension
{
    String  prefix              = 'META-INF'
    boolean includeSCM          = true
    boolean includeEnv          = false
    boolean includeSystem       = false
    boolean includeProperties   = false
    boolean includePaths        = false
    Object  includeDependencies = false
    boolean gitStatusProject    = true
    String  endOfLine           = 'windows'

    String  fileName
    File    directory
    String  include             = '*.jar, *.war, *.ear, *.zip'
    String  exclude

    /**
     * Pattern for finding out the configuration name in the dependencies report
     */
    final Pattern configurationNamePattern = Pattern.compile( /^(\w+) - / )
}
