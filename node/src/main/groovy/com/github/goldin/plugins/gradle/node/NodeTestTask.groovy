package com.github.goldin.plugins.gradle.node

import org.gcontracts.annotations.Ensures
import org.gradle.api.tasks.testing.Test


/**
 * Tests Node.js application.
 */
class NodeTestTask extends NodeBaseTask
{

    @Override
    void nodeTaskAction()
    {
        final binFolder = new File( './node_modules/.bin' )
        assert binFolder.directory, "[$binFolder] not found"

        //  > $testResultsDir.canonicalPath/TEST-${ project.group }-${ project.name }.xml
        final testResultsDir = testResultsDir()
        final testScript     = """
#!/bin/bash

source \${0%/*}/${ NodeConstants.SETUP_SCRIPT }
export PATH=$binFolder:\$PATH

echo "Running '$ext.testCommand'"
$ext.testCommand${ ext.testCommand.startsWith( 'mocha' ) ? ' -R xunit' : '' }
"""
        final testOutput = bashExec( testScript, "$project.buildDir/${ NodeConstants.TEST_SCRIPT }" )
        final j = 5
    }


    @Ensures({ result.directory })
    File testResultsDir()
    {
        final Test testTask       = ( Test ) project.tasks.asMap[ 'test' ]
        final File testResultsDir = testTask?.testResultsDir ?: new File( 'build/test-results' )
        assert testResultsDir.with { directory || mkdirs() }
        testResultsDir
    }
}
