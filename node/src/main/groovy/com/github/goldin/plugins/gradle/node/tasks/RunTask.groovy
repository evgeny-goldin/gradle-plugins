package com.github.goldin.plugins.gradle.node.tasks


/**
 * Runs commands specified.
 */
class RunTask extends NodeBaseTask
{
    @Override
    @SuppressWarnings([ 'UseCollectMany' ])
    void taskAction()
    {
        assert ext.run, 'No commands to run are specified'
        bashExec( commandsScript( ext.run.collect{[ "echo $it", "$it${ ext.removeColorCodes }".toString() ]}.flatten(), 'run' ), taskScriptFile(), false, true, false )
    }
}
