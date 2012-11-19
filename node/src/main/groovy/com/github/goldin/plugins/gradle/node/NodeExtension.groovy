package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    List<String> cleanWorkspaceCommands = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace         = false // Whether to run cleanWorkspaceCommands before running other tasks
    boolean      cleanNodeModules       = false // Whether to remove "./node_modules" before running other tasks
    boolean      failOnTestFailures     = true  // Whether to fail execution if tests fail

    boolean      echoCommands           = true  // Echo all commands executed in a bash script
    boolean      echoOutput             = false // Echo all bash output to the build log, Maven-like
    boolean      teamCityTests          = false // Whether test results should be displayed using TeamCity service messages (when job is run by TeamCity)

    String  NODE_ENV     = 'development'
    String  nodeVersion  = 'latest'
    String  testCommand  = 'mocha'
    String  startCommand = 'forever server.js'

    Map<String, Map<String, Object>> configs             = [:]
    String                           configsKeyDelimiter = '.'
}
