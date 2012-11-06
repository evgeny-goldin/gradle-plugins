package com.github.goldin.plugins.gradle.about

import java.util.regex.Pattern


class AboutExtension
{
    String  prefix              = 'META-INF'
    String  endOfLine           = 'linux'
    boolean includeSCM          = true
    boolean includeEnv          = false
    boolean includeSystem       = false
    boolean includeProperties   = false
    boolean includePaths        = false
    Object  includeDependencies = false

    String  fileName
    File    directory
    String  include = '**/*.jar, **/*.war, **/*.ear, **/*.zip'
    String  exclude

    /**
     * Pattern for finding out the configuration name in the dependencies report
     */
    final Pattern configurationNamePattern = Pattern.compile( /^(\w+) - / )
}
