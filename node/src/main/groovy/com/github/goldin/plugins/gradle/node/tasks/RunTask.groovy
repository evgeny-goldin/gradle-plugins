package com.github.goldin.plugins.gradle.node.tasks


/**
 * Runs commands specified.
 */
class RunTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        assert ext.run, "No commands to run are specified"
        bashExec( beforeAfterScript( ext.run, 'run' ), taskScriptFile(), false, true, false )
    }
}
