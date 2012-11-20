package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    List<String> cleanWorkspaceCommands = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace         = false // Whether to run cleanWorkspaceCommands before running tasks
    boolean      failOnTestFailures     = true  // Whether to fail execution if tests fail
    boolean      generateOnly           = false // Whether bash scripts should be generated but not run

    boolean      echoCommands           = true  // Echo all commands executed
    boolean      echoOutput             = false // Echo all scripts output to the build log, Maven-like
    boolean      teamCityTests          = false // Whether test results should be written to log as TeamCity service messages

    String  NODE_ENV     = 'development'
    String  nodeVersion  = 'latest'
    String  testCommand  = 'mocha'
    String  startCommand = 'forever start server.js'

    Map<String, Map<String, Object>> configs             = [:]
    String                           configsKeyDelimiter = '.'
}
