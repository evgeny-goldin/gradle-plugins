package com.github.goldin.plugins.gradle.node

import com.github.goldin.plugins.gradle.common.extensions.ShellExtension


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName', 'DuplicateListLiteral' ])
class NodeExtension extends ShellExtension
{
    boolean      updated                  = false // Internal property, set to 'true' after extension is updated
    List<String> cleanWorkspaceCommands   = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace           = false // Whether to run cleanWorkspaceCommands before running tasks

    String       NODE_ENV                 = 'development'
    String       nodeVersion              = 'latest'
    String       testCommand              = 'mocha'
    String       testInput                = 'test'

    String       foreverOptions                   // Additional command-line 'forever' options, such as '-w -v'
    boolean      configMergePreserveOrder = true  // Whether configs merge should preserve keys order (more risky, some nasty regexes are involved)
    boolean      xUnitReport              = true  // Whether xUnit report should be created when tests are run
    String       xUnitReportFile          = 'TEST-node.xml'  // xUnit report file name written to the test-results directory
    List<String> addTasks                 = null  // Which tasks should be added to the project, all tasks are added if null, no tasks are added if empty
    boolean      ensureForever            = true  // Whether 'forever' should be installed even if it doesn't appear in 'package.json'
    boolean      failIfNoPid              = true  // Whether to fail execution if no PID file was found after application has started
    boolean      failIfNoTests            = true  // Whether to fail execution if no tests were found
    boolean      failIfTestsFail          = true  // Whether to fail execution if tests fail
    boolean      stopallBeforeStart       = false // Whether 'stopall' should run before 'start'

    boolean      checkAfterRestartall     = true  // Whether 'checkStarted' should run after 'restartall'
    boolean      checkAfterStopall        = true  // Whether 'checkStopped' should run after 'stopall'
    boolean      listAfterStart           = true  // Whether 'list' should run after 'start'
    boolean      listAfterRestartall      = true  // Whether 'list' should run after 'restartall'
    boolean      listAfterStop            = true  // Whether 'list' should run after 'stop'
    boolean      listAfterStopall         = true  // Whether 'list' should run after 'stopall'
    boolean      npmCleanInstall          = false // Internal property, whether 'npm install' was run on a clean directory
    boolean      npmLocalCache            = true  // Whether results of 'npm install' are cached locally
    boolean      npmInstallDevDependencies = true // Whether 'devDependencies' should be installed when "npm install" is running
    String       npmRemoteCache                   // Remote repo URL for storing 'npm install' cache archives
    boolean      pidOnlyToStop            = true  // Whether 'stop' task can only use a valid .pid file (created by 'start') and no 'kill' operations
    String       pidFileName                      // PID file name
    int          port                     = 1337  // Port the application starts on (becomes part of PID file name)

    String       printUrl                 = '/'   // Application's URL to print after it has started. Nothing is displayed if set to '' or null
    boolean      printPublicIp            = true  // Whether public IP of application is printed if 'printUrl' is used
    String       publicIp                         // Internal property, public IP resolved

    String        scriptPath
    List<String>  knownScriptPaths        = 'server.js server.coffee app.js app.coffee'.tokenize().asImmutable()
    String        scriptArguments                 // Additional script arguments
    List<String>  run                     = []    // Commands to execute by 'run' task, if defined application is not started or stopped
    List<String>  before                  = []    // Commands to execute before running unit tests or starting the application
    List<String>  after                   = []    // Commands to execute after running unit tests or stopping the application
    List<String>  beforeStart             = []    // Commands to execute before starting the application
    List<String>  afterStop               = []    // Commands to execute after stopping the application
    List<String>  beforeTest              = []    // Commands to execute before running unit tests
    List<String>  afterTest               = []    // Commands to execute after running unit tests

    int           redisPort               = -1    // Local Redis instance port number to start and stop
    String        redisPortConfigKey              // Config key holding Redis port number to start and stop
    String        redisCommandLine                // Additional Redis command-line arguments
    boolean       redisStartInProduction  = false // Whether Redis should be started when NODE_ENV=production
    boolean       redisStopInProduction   = false // Whether Redis should be stopped when NODE_ENV=production
    int           redisWait               = 5     // Seconds to wait after Redis has started or stopped and checking it
    List<String>  redisListeners          = [ 'before', 'after' ] // List of listeners for Redis to start or stop.
                                                                  // Possible options are: 'before', 'beforeStart', 'beforeTest',
                                                                  //                       'after',  'afterStop',   'afterTest'

    int           mongoPort               = -1    // Local MongoDB instance port number to start and stop
    String        mongoPortConfigKey              // Config key holding MongoDB port number to start and stop
    String        mongoCommandLine                // Additional Mongo command-line arguments
    String        mongoLogpath
    String        mongoDBPath
    boolean       mongoStartInProduction  = false // Whether MongoDB should be started when NODE_ENV=production
    boolean       mongoStopInProduction   = false // Whether MongoDB should be stopped when NODE_ENV=production
    int           mongoWait               = 5     // Seconds to wait after MongoDB has started or stopped and checking it
    List<String>  mongoListeners          = [ 'before', 'after' ] // List of listeners for Mongo to start or stop.
                                                                  // Possible options are: 'before', 'beforeStart', 'beforeTest',
                                                                  //                       'after',  'afterStop',   'afterTest'


    List <Map<String, ?>> configs            = []     // List of config maps to update project files with. Every map is:
                                                      // Key   - path of destination JSON config file to update or create
                                                      // Value - config data Map ( key => value ) or existing JSON / .properties File
    String               configsNewKeys      = 'fail' // 'fail', 'ignore' or 'create' - action to be taken when configs merge brings new keys into destination file
    String               configsKeyDelimiter = '.'
    boolean              printConfigs        = false  // Whether resulting configs should be printed after merge operation
    List<String>         printConfigsMask    = [ 'password' ] // Config properties to mask when printed

    List <Map<String, ?>> configsResult             // Internal property, configs resulting from merging external configs with those of the project


    List <Map<String, Map<String, String>>> replaces = []   // List of replace maps to update project files with. Every map is:
                                                            // Key   - path of destination file to update (should exist)
                                                            // Value - Map of replacements to make:
                                                            //         Key   - replacement regex /pattern/ or regular 'value'
                                                            //         Value - value to replace the pattern to
}
