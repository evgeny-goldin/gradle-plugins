package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.extensions.BaseExtension
import org.gradle.api.tasks.bundling.Zip
import java.util.regex.Pattern


class AboutExtension extends BaseExtension
{
    String    prefix              = 'META-INF'
    String    endOfLine           = 'linux'
    boolean   includeSCM          = true
    boolean   includeEnv          = false
    boolean   includeSystem       = false
    boolean   includeProperties   = false
    boolean   includePaths        = false
    Object    includeDependencies = false
    boolean   failIfNotFound      = true
    boolean   failOnError         = true
    boolean   zipTasks            = true
    boolean   patterns            = true
    List<Zip> tasks               = []

    String  fileName
    File    directory
    String  include = '**/*.jar, **/*.war, **/*.ear, **/*.zip'
    String  exclude

    /**
     * Pattern for finding out the configuration name in the dependencies report
     */
    final Pattern configurationNamePattern = ~/^(\w+) - /
}
