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
    String  stopCommand  = 'forever stopall'
    String  startCommand = 'forever start server.js'
    String  listCommand  = 'forever list'

    List <Map<String,?>> configs             = []   // List of config maps. Every map is:
                                                    // Key   - path of destination JSON config file to update or generate
                                                    // Value - existing JSON / .properties File to read or config data Map
    boolean              configsUpdateOnly   = true
    String               configsKeyDelimiter = '.'
}
