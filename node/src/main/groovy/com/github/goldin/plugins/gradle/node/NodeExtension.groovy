package com.github.goldin.plugins.gradle.node


@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention', 'PropertyName' ])
class NodeExtension
{
    boolean cleanWorkspace   = true // git checkout -f + git clean -dff
    boolean cleanNodeModules = true // rm -rf ./node_modules
    String  NODE_ENV         = 'development'
}
