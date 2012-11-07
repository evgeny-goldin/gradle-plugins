package com.github.goldin.plugins.gradle.about

import org.gradle.api.tasks.bundling.AbstractArchiveTask

import java.util.regex.Pattern


class AboutExtension
{
    String                    prefix              = 'META-INF'
    String                    endOfLine           = 'linux'
    boolean                   includeSCM          = true
    boolean                   includeEnv          = false
    boolean                   includeSystem       = false
    boolean                   includeProperties   = false
    boolean                   includePaths        = false
    Object                    includeDependencies = false
    boolean                   failIfNotFound      = true
    boolean                   failOnError         = true
    boolean                   gradleTasks         = true
    boolean                   patterns            = false
    List<AbstractArchiveTask> archiveTasks        = []

    String  fileName
    File    directory
    String  include = '**/*.jar, **/*.war, **/*.ear, **/*.zip'
    String  exclude

    /**
     * Pattern for finding out the configuration name in the dependencies report
     */
    final Pattern configurationNamePattern = Pattern.compile( /^(\w+) - / )
}
