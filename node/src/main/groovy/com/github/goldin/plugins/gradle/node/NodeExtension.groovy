package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    boolean      updated                  = false // Internal property, set to 'true' after extension is updated
    List<String> cleanWorkspaceCommands   = [ 'git checkout -f', 'git clean -dff' ]
    boolean      cleanWorkspace           = false // Whether to run cleanWorkspaceCommands before running tasks
    String       shell                    = '/bin/bash'
    String       NODE_ENV                 = 'development'
    String       nodeVersion              = 'latest'
    String       testCommand              = 'mocha'
    String       testInput                = 'test'

    /**
     * Whether color codes should be removed from command outputs.
     */
    boolean      removeColor              = 'BUILD_NUMBER JENKINS_URL TEAMCITY_VERSION'.split().any{ System.getenv( it ) != null }

    String       foreverOptions                   // Additional command-line 'forever' options, such as '-w -v'
    String       removeColorCodes                 // Internal property
    boolean      configMergePreserveOrder = true  // Whether configs merge should preserve keys order (more risky, some nasty regexes are involved)
    boolean      xUnitReport              = true  // Whether xUnit report should be created when tests are run
    String       xUnitReportFile          = 'TEST-node.xml'  // xUnit report file name written to the test-results directory
    boolean      failIfNoTests            = true  // Whether to fail execution if no tests were found
    boolean      failIfTestsFail          = true  // Whether to fail execution if tests fail
    boolean      stopIfFailsToStart       = true  // Whether the app should be stopped if it fails to start
    boolean      stopBeforeStart          = true  // Whether 'stop' should run before 'start'
    boolean      stopallBeforeStart       = false // Whether 'stopall' should run before 'start'
    boolean      checkAfterStart          = true  // Whether 'checkStarted' should run after 'start'
    boolean      checkAfterRestartall     = true  // Whether 'checkStarted' should run after 'restartall'
    boolean      checkAfterStop           = true  // Whether 'checkStopped' should run after 'stop'
    boolean      checkAfterStopall        = true  // Whether 'checkStopped' should run after 'stopall'
    boolean      npmCleanInstall          = false // Internal property, whether 'npm install' was run on a clean directory
    boolean      npmLocalCache            = true  // Whether results of 'npm install' are cached locally
    boolean      npmInstallDevDependencies = true // Whether 'devDependencies' should be installed when "npm install" is running
    String       npmRemoteCache                   // Remote repo URL for storing 'npm install' cache archives
    boolean      startupScript            = false // Whether a startup script should be created in 'build' directory
    boolean      pidOnlyToStop            = true  // Whether 'stop' task can only use a valid .pid file (created by 'start') and no 'kill' operations
    String       pidFileName                      // PID file name
    int          portNumber               = 1337  // Port the application starts on (becomes part of .pid file name)

    Map<String,Object> env                = [:]   // Environment variables to set before application is started
    String       printUrl                 = '/'   // Application's URL to print after it has started. Nothing is displayed if set to '' or null
    boolean      printPublicIp            = true  // Whether public IP of application is printed if 'printUrl' is used
    String       publicIp                         // Internal property, public IP resolved

    /**
     * Checks to perform after application has started.
     * Key  : application url, relative like '/' or '/login' or full like 'http://...'
     * Value: two elements list: [0] - expected status code, int
     *                           [1] - expected content, String
     *                                 Same format as in "monitor" plugin - 'text', '/regex/', '[json]', '{json}', token1*token2
     */
    Map<String,List<?>> checks            = [ '/' : [ 200, '' ]]

    int          checkWait                = 5     // Seconds to wait after starting/stopping the application and checking it
    int          checkTimeout             = 10    // Seconds to wait for check test to succeed or fail (timeout)

    List<Closure> transformers            = []    // Callbacks to invoke after every shell script is generated
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

    int           mongoPort               = -1    // Local MongoDB instance port number to start and stop
    String        mongoPortConfigKey              // Config key holding MongoDB port number to start and stop
    String        mongoCommandLine                // Additional Mongo command-line arguments
    String        mongoLogpath
    String        mongoDBPath
    boolean       mongoStartInProduction  = false // Whether MongoDB should be started when NODE_ENV=production
    boolean       mongoStopInProduction   = false // Whether MongoDB should be stopped when NODE_ENV=production
    int           mongoWait               = 5     // Seconds to wait after MongoDB has started or stopped and checking it


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
