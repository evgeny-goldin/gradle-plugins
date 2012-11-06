package com.github.goldin.plugins.gradle.about


class AboutExtension
{
    String  prefix              = 'META-INF'
    boolean includeSCM          = true
    boolean includeEnv          = false
    boolean includeSystem       = false
    boolean includeProperties   = false
    boolean includePaths        = false
    boolean includeDependencies = false
    boolean gitStatusProject    = true
    String  endOfLine           = 'windows'

    String  fileName
    File    directory
    String  include             = '*.jar, *.war, *.ear, *.zip'
    String  exclude
}
