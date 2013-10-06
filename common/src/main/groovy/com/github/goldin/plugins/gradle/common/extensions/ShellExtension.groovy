package com.github.goldin.plugins.gradle.common.extensions


/**
 * Base class for extension describing plugin generating shell scripts
 */
class ShellExtension extends BaseExtension
{
    String        shell          = '/bin/bash'
    List<Closure> transformers   = []    // Callbacks to invoke after every shell script is generated
    boolean       verbose        = false // Whether scripts generated should have 'set -x' added (will print out every command executed)
    boolean       startupScript  = false // Whether a startup script should be created in 'build' directory
    Map<String,Object> env       = [:]   // Environment variables to set before application is started

    /**
     * Whether color codes should be removed from command outputs.
     */
    boolean      removeColor     = 'BUILD_NUMBER JENKINS_URL TEAMCITY_VERSION'.split().any{ System.getenv( it ) != null }
    String       removeColorCodes // Internal property

    boolean      stopBeforeStart    = true  // Whether 'stop' should run before 'start'
    boolean      stopIfFailsToStart = true  // Whether the app should be stopped if it fails to start
    boolean      checkAfterStart    = true  // Whether 'checkStarted' task should run after 'start'
    boolean      checkAfterStop     = true  // Whether 'checkStopped' task should run after 'stop'

    /**
     * Checks to perform after application has started.
     * Key  : application url, relative like '/' or '/login' or full like 'http://host:port/url'
     * Value: two elements list: [0] - expected status code, int
     *                           [1] - expected content, String
     *                                 Same format as in "monitor" plugin - 'text', '/regex/', '[json]', '{json}', token1*token2
     */
    Map<String,List<?>> checks   = [ '/' : [ 200, '' ]]
    int          checkWait       = 5     // Seconds to wait after starting/stopping the application and checking it
    int          checkTimeout    = 10    // Seconds to wait for check test to succeed or fail (timeout)
}
