package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    List<String> cleanWorkspaceCommands   = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace           = false // Whether to run cleanWorkspaceCommands before running tasks
    String       NODE_ENV                 = 'development'
    String       nodeVersion              = 'latest'
    String       testCommand              = 'mocha'
    String       testInput                = 'test'
    boolean      configMergePreserveOrder = true  // Whether configs merge should preserve keys order (more risky, some nasty regexes are involved)
    boolean      xUnitReport              = true  // Whether xUnit report should be created when tests are run
    boolean      failIfTestsFail          = true  // Whether to fail execution if tests fail
    boolean      stopIfFailsToStart       = true  // Whether the app should be stopped if it fails to start
    boolean      stopBeforeStart          = true  // Whether 'stop' should run before 'start'
    boolean      checkAfterStart          = true  // Whether 'checkStarted' should run after 'start'
    boolean      checkAfterRestartall     = true  // Whether 'checkStarted' should run after 'restartall'
    boolean      checkAfterStop           = true  // Whether 'checkStopped' should run after 'stop'
    boolean      checkAfterStopall        = true  // Whether 'checkStopped' should run after 'stopall'
    boolean      pidOnlyToStop            = true  // Whether 'stop' task can only use a valid .pid file (created by 'start') and no 'kill' operations
    String       pidFileName              = ''    // PID file name
    int          portNumber               = 1337  // Port the application starts on (becomes part of .pid file name)

    String       printUrl                 = '/'   // The URL to print after the application has started. Nothing is displayed if set to '' or null
    String       checkUrl                         // The URL to check after application has started, "http://127.0.0.1:$portNumber" by default
    String       checkContent             = ''    // Response to expect when making a request
    int          checkStatusCode          = 200   // Response code to expect when making a request
    int          checkWait                = 3     // Seconds to wait after starting/stopping the application and checking it
    int          checkTimeout             = 10    // Seconds to wait for check test to succeed or fail (timeout)

    List<Closure> transformers            = []    // Callbacks to invoke after every bash script is generated
    String        foreverOptions          = ''    // Additional command-line 'forever' options, such as '-w -v'
    String        scriptPath
    String        scriptArguments         = ''    // Additional script arguments when run by 'forever'
    List<String>  run                     = []    // Commands to execute by 'run' task, if defined application is not started or stopped
    List<String>  before                  = []    // Commands to execute before running unit tests or starting the application
    List<String>  after                   = []    // Commands to execute after running unit tests or stopping the application

    int           redisPort               = -1    // Local Redis instance port number to start and stop
    String        redisPortConfigKey              // Config key holding Redis port number to start and stop
    String        redisCommandLine                // Additional Redis command-line arguments
    boolean       redisStartInProduction  = false // Whether Redis should be started when NODE_ENV=production
    boolean       redisStopInProduction   = false // Whether Redis should be stopped when NODE_ENV=production
    int           redisWait               = 3     // Seconds to wait after Redis has started or stopped and checking it
    boolean       redisAddedAlready       = false // Internal property, whether Redis commands are already added to before/after

    int           mongoPort               = -1    // Local MongoDB instance port number to start and stop
    String        mongoPortConfigKey      = ''    // Config key holding MongoDB port number to start and stop
    String        mongoCommandLine                // Additional Mongo command-line arguments
    String        mongoLogpath
    String        mongoDBPath
    boolean       mongoStartInProduction  = false // Whether MongoDB should be started when NODE_ENV=production
    boolean       mongoStopInProduction   = false // Whether MongoDB should be stopped when NODE_ENV=production
    int           mongoWait               = 3     // Seconds to wait after MongoDB has started or stopped and checking it
    boolean       mongoAddedAlready       = false // Internal property, whether MongoDB commands are already added to before/after


    List <Map<String, ?>> configs            = []     // List of config maps to update project files with. Every map is:
                                                      // Key   - path of destination JSON config file to update or create
                                                      // Value - config data Map ( key => value ) or existing JSON / .properties File
    String               configsNewKeys      = 'fail' // 'fail', 'ignore' or 'create' - action to be taken when configs merge brings new keys
    String               configsKeyDelimiter = '.'
    boolean              printConfigs        = false  // Whether resulting configs should be printed after merge operation

    List <Map<String, ?>> configsResult             // Internal property, configs resulting from merging external configs with those of the project


    List <Map<String, Map<String, String>>> replaces = []   // List of replace maps to update project files with. Every map is:
                                                            // Key   - path of destination file to update (should exist)
                                                            // Value - Map of replacements to make:
                                                            //         Key   - replacement regex /pattern/ or regular 'value'
                                                            //         Value - value to replace the pattern to
}
