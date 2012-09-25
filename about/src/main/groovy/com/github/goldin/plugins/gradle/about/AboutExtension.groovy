package com.github.goldin.plugins.gradle.about


class AboutExtension
{
    String  fileName
    String  prefix           = 'META-INF'
    boolean dumpSCM          = true
    boolean dumpEnv          = false
    boolean dumpSystem       = false
    boolean dumpPaths        = false
    boolean dumpDependencies = false
    boolean gitStatusProject = true
    String  endOfLine        = 'windows'
    File    directory
    String  include          = '*.jar'
    String  exclude
}
