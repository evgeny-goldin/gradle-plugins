package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    boolean cleanWorkspace     = true  // git checkout -f + git clean -dff
    boolean cleanNodeModules   = true  // rm -rf ./node_modules
    boolean echoCommands       = true  // Echo all commands executed
    boolean teamCityTests      = false // Whether test results should be displayed using TeamCity service messages (when job is run by TeamCity)
    boolean failOnTestFailures = true  // Whether execution should fail when there are test failures.

    String  NODE_ENV           = 'development'
    String  nodeVersion        = 'latest'
    String  testCommand        = 'mocha'
    String  startCommand       = 'forever server.js'
}
