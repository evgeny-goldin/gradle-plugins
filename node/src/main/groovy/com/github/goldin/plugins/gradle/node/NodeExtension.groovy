package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    List<String> cleanWorkspaceCommands = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace         = false // Whether to run cleanWorkspaceCommands before running tasks
    boolean      xUnitReport            = true  // Whether xUnit report should be created when tests are run
    boolean      failIfTestsFail        = true  // Whether to fail execution if tests fail
    boolean      stopIfFailsToStart     = true  // Whether the app should be stopped if it fails to start
    boolean      stopAndStart           = true  // Whether 'start' should be preceded by 'stop'
    boolean      startAndCheck          = true  // Whether 'start' should be followed by 'check'
    boolean      usePidOnlyToStop       = true  // Whether 'stop' task can only use a valid .pid file (created by 'start') and no 'kill' operations
    boolean      startWithForever       = true  // Whether 'forever' should be used to start the application
    int          portNumber             = 1337                           // Port the application starts on (becomes part of .pid file name)
    String       checkUrl               = "http://127.0.0.1:$portNumber" // The URL to check after application has started
    long         checkDelay             = 1000                           // Amount of milliseconds to wait before making a connection
    String       checkContent           = ''                             // Response to expect when making a request
    int          checkStatusCode        = 200                            // Response code to expect when making a request

    List<Closure> transformers          = [] // Callbacks to invoke when every bash script is generated
    String        NODE_ENV              = 'development'
    String        nodeVersion           = 'latest'
    String        testCommand           = 'mocha'
    String        testInput             = 'test'

    String        scriptPath            = 'server.js'
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
