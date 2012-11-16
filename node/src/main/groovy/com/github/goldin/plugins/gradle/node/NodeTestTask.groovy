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
        final binFolder = new File( NodeConstants.NODE_MODULES_BIN )
        assert binFolder.directory, "[$binFolder] not found"

        final isMocha    = ext.testCommand.startsWith( 'mocha' )
        final testScript = """
#!/bin/bash

source \${0%/*}/${ NodeConstants.SETUP_SCRIPT }
export PATH=$binFolder:\$PATH

echo "Running '$ext.testCommand'"
$ext.testCommand${ isMocha ? ' -R xunit' : '' }
"""
        String testOutput = bashExec( testScript, "$project.buildDir/${ NodeConstants.TEST_SCRIPT }" )

        if ( isMocha )
        {
            final  testSuiteStart = testOutput.indexOf( '<testsuite' )
            assert testSuiteStart > -1, "Failed to find '<testsuite' in Mocha test results\n[$testOutput]"
            testOutput = testOutput.substring( testSuiteStart )
        }

        new File( "${ testResultsDir().canonicalPath }/TEST-${ project.group }-${ project.name }.xml" )


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
