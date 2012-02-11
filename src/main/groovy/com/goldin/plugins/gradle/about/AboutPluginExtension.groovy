package com.goldin.plugins.gradle.about


/**
 * {@link AboutPlugin} extension object.
 * http://gradle.org/docs/1.0-milestone-7/userguide/custom_plugins.html#N14C69
 */
class AboutPluginExtension
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
