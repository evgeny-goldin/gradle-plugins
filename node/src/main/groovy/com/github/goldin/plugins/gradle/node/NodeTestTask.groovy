package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.tasks.testing.Test


/**
 * Tests Node.js application.
 */
class NodeTestTask extends NodeBaseTask
{

    @Override
    void nodeTaskAction()
    {
        String teamCityReport = bashExec( testScript(), "$project.buildDir/$TEST_SCRIPT", false )
        String xunitReport    = teamCityReportToXUnit( teamCityReport.readLines()*.trim().grep().findAll { it.startsWith( '##teamcity' )} )

        new File( "${ testResultsDir().canonicalPath }/test-report.xml" ).
        write( xunitReport, 'UTF-8' )
    }


    @Ensures({ result })
    private String testScript()
    {
        final binFolder = new File( NODE_MODULES_BIN )
        assert binFolder.directory, "[$binFolder] not found"

        final  isMocha  = ext.testCommand.startsWith( 'mocha' )
        assert isMocha, "Only 'mocha' test runner is currently supported"

        """#!/bin/bash

        source \${0%/*}/$SETUP_SCRIPT
        export PATH=$binFolder:\$PATH

        echo "Running '$ext.testCommand'"
        $ext.testCommand${ isMocha ? ' -R teamcity' : '' }""".stripIndent()
    }


    @Ensures({ result.directory })
    private File testResultsDir()
    {
        final testTask            = project.tasks.findByName( 'test' )
        final File testResultsDir = ( testTask instanceof Test ) ? (( Test ) testTask ).testResultsDir : new File( 'build/test-results' )
        assert testResultsDir.with { directory || mkdirs() }
        testResultsDir
    }


    @Requires({ teamCityReport })
    @Ensures({ result })
    private String teamCityReportToXUnit ( List<String> teamCityReport )
    {
        assert teamCityReport.every { it.startsWith( '##teamcity[' )}
        assert teamCityReport[  0 ].startsWith     ( '##teamcity[testSuiteStarted name=\'' )
        assert teamCityReport[ -1 ].startsWith     ( '##teamcity[testSuiteFinished name=\'' )

        final report   = []
        int   tests    = 0
        int   failures = 0
        int   skipped  = 0

        for ( line in teamCityReport[ 1 .. -2 ] )
        {
            final testName = find( line, NameAttributePattern ).replace( '"', '\\"' )

            if ( line.startsWith( '##teamcity[testFinished ' ))
            {
                tests++
                report << """<testcase name="$testName" time="${ ( find( line, DurationAttributePattern ) as int ) / 1000 }"/>"""
            }
            else if ( line.startsWith( '##teamcity[testFailed ' ))
            {
                failures++
                report << """<testcase name="$testName"><failure message="${ find( line, MessageAttributePattern ).replace( '"', '\\"' ) }"/></testcase>"""
            }
            else if ( line.startsWith( '##teamcity[testIgnored ' ))
            {
                skipped++
                report << """<testcase name="$testName"><skipped/></testcase>"""
            }
        }

        """
<testsuite name="${ find( teamCityReport[ 0 ], NameAttributePattern )}" tests="$tests" failures="$failures" skip="$skipped">
    ${ report.join( '\n    ' ) }
</testsuite>
""".trim()
    }
}
