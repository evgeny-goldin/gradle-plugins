package com.goldin.plugins.gradle.about


/**
 * {@link AboutPlugin} convention object.
 */
class AboutPluginConvention
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
