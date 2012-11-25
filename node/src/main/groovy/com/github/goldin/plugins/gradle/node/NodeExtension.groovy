package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    List<String> cleanWorkspaceCommands = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace         = false // Whether to run cleanWorkspaceCommands before running tasks
    boolean      failOnTestFailures     = true  // Whether to fail execution if tests fail
    boolean      generateOnly           = false // Whether bash scripts should be generated but not run
    boolean      global                 = false // Whether Node.js settings are applied globally

    boolean      startDependsOnStop     = true  // Whether 'start' task should depend on the 'stop' task
    boolean      echoScripts            = false // Echo all bash scripts generated to the build log
    boolean      echoCommands           = false // Echo all commands executed in bash scripts to the build log
    boolean      teamCityTests          = false // Whether test results should be written to log as TeamCity service messages

    List<Closure> transformers  = []  // Callbacks to invoke when every bash script is generated
    String        NODE_ENV      = 'development'
    String        nodeVersion   = 'latest'
    String        testCommand   = 'mocha'
    String        testInput     = 'test'

    String        scriptPath    = 'server.js'
    boolean       isCoffee
    List<String>  stopCommands
    List<String>  startCommands

    List <Map<String,?>> configs             = []   // List of config maps. Every map is:
                                                    // Key   - path of destination JSON config file to update or generate
                                                    // Value - existing JSON / .properties File to read or config data Map
    boolean              configsUpdateOnly   = true
    String               configsKeyDelimiter = '.'
}
