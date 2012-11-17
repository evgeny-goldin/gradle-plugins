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
        String testOutput = bashExec( testScript, "$project.buildDir/${ NodeConstants.TEST_SCRIPT }", false )

        if ( isMocha )
        {
            final  testSuiteStart = testOutput.indexOf( '<testsuite' )
            assert testSuiteStart > -1, "Failed to find '<testsuite' in Mocha test results\n[$testOutput]"
            testOutput = testOutput.substring( testSuiteStart )
        }

        new File( "${ testResultsDir().canonicalPath }/mocha-report.xml" ).
        write( testOutput, 'UTF-8' )
    }


    @Ensures({ result.directory })
    File testResultsDir()
    {
        final testTask            = project.tasks.findByName( 'test' )
        final File testResultsDir = ( testTask instanceof Test ) ? (( Test ) testTask ).testResultsDir : new File( 'build/test-results' )
        assert testResultsDir.with { directory || mkdirs() }
        testResultsDir
    }
}
