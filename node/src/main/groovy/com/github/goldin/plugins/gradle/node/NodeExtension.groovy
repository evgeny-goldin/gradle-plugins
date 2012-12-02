package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    List<String> cleanWorkspaceCommands = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace         = false // Whether to run cleanWorkspaceCommands before running tasks
    boolean      failOnTestFailures     = true  // Whether to fail execution if tests fail
    boolean      generateOnly           = false // Whether bash scripts should be generated but not run

    boolean      startDependsOnStop     = true  // Whether 'start' task should depend on the 'stop' task
    boolean      echoScripts            = false // Echo all bash scripts generated to the build log
    boolean      echoCommands           = false // Echo all commands executed in bash scripts to the build log
    boolean      teamCityTests          = false // Whether test results should be written to log as TeamCity service messages

    String       startCheckUrl          = 'http://127.0.0.1:1337' // The URL to test after application has started
    long         startCheckDelay        = 1000                    // If 'startCheckUrl' - amount of milliseconds to wait before making a connection
    String       startCheckResponse     = ''                      // If 'startCheckUrl' - response to expect when making a request
    int          startCheckResponseCode = 200                     // If 'startCheckUrl' - response code to expect when making a request

    List<Closure> transformers          = []  // Callbacks to invoke when every bash script is generated
    String        NODE_ENV              = 'development'
    String        nodeVersion           = 'latest'
    String        testCommand           = 'mocha'
    String        testInput             = 'test'

    String        scriptPath            = 'server.js'
    boolean       isCoffee
    List<String>  stopCommands
    List<String>  startCommands

    List <Map<String,?>> configs             = []   // List of config maps. Every map is:
                                                    // Key   - path of destination JSON config file to update or create
                                                    // Value - config data Map ( key => value ) or existing JSON / .properties File
    boolean              configsUpdateOnly   = true
    String               configsKeyDelimiter = '.'

    List <Map<String,Map<String, String>>> replaces = []   // List of replace maps. Every map is:
                                                           // Key   - path of destination file to update (should exist)
                                                           // Value - Map of replacements to make:
                                                           //         Key   - replacement regex /pattern/ or regular 'value'
                                                           //         Value - value to replace the pattern to
}
