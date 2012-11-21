package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test
import java.util.regex.Pattern


/**
 * Tests Node.js application.
 */
class NodeTestTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        final testReport = bashExec( testScript(), scriptPath( TEST_SCRIPT ), false, ext.generateOnly )

        if ( ! ext.generateOnly )
        {
            final teamCityReport = testReport.readLines()*.trim().grep().findAll { it.startsWith( '##teamcity' )}
            writeXUnitReport( teamCityReport, new File( "${ testResultsDir().canonicalPath }/TEST-node.xml" ))
        }
    }


    @Ensures({ result })
    private String testScript()
    {
        final  isMocha  = ext.testCommand.startsWith( 'mocha' )
        assert isMocha, "Only 'mocha' test runner is currently supported"

        """
        ${ bashScript()}

        $ext.testCommand${ isMocha ? ' -R teamcity' : '' }""".stripIndent()
    }


    @Ensures({ result.directory })
    private File testResultsDir()
    {
        final  testTask       = project.tasks.findByName( 'test' )
        final  testResultsDir = ( testTask instanceof Test ) ? (( Test ) testTask ).testResultsDir : new File( 'build/test-results' )
        assert testResultsDir.with { directory || mkdirs() }
        testResultsDir
    }


    @Requires({ teamCityReport && reportFile })
    private void writeXUnitReport ( List<String> teamCityReport, File reportFile )
    {
        assert teamCityReport.every { it.startsWith( '##teamcity[' )}
        assert teamCityReport[  0 ].startsWith     ( '##teamcity[testSuiteStarted name=\'' )
        assert teamCityReport[ -1 ].startsWith     ( '##teamcity[testSuiteFinished name=\'' )

        // Example line:
        // ##teamcity[testFailed name='RTB - Campaign "before all" hook' message='ER_ACCESS_DENIED_ERROR: Access denied for user |'root|'@|'localhost|' (using password: NO)']
        final attribute   = { String line, Pattern p -> find( line, p ).replace( "|'", "'"    ).
                                                                        replace( '"',  "'"    ).
                                                                        replace( '<',  '&lt;' ).
                                                                        replace( '>',  '&gt;' )}
        final reportLines = []
        int   tests       = 0
        int   failures    = 0
        int   skipped     = 0

        for ( line in teamCityReport[ 1 .. -2 ] )
        {
            final testName = attribute( line, NameAttributePattern )

            if ( line.startsWith( '##teamcity[testFinished ' ))
            {
                tests++
                reportLines << """<testcase name="$testName" time="${ ( attribute( line, DurationAttributePattern ) as int ) / 1000 }"/>"""
            }
            else if ( line.startsWith( '##teamcity[testFailed ' ))
            {
                failures++
                reportLines << """<testcase name="$testName"><failure message="${ attribute( line, MessageAttributePattern ) }"/></testcase>"""
            }
            else if ( line.startsWith( '##teamcity[testIgnored ' ))
            {
                skipped++
                reportLines << """<testcase name="$testName"><skipped/></testcase>"""
            }
        }

        final String xUnitReport = """
<testsuite name="${ attribute( teamCityReport[ 0 ], NameAttributePattern )}" tests="$tests" failures="$failures" skip="$skipped">
    ${ reportLines.join( '\n    ' ) }
</testsuite>""".trim()

        assert reportFile.with { ( ! file ) || ( project.delete( delegate )) }
        reportFile.write( xUnitReport, 'UTF-8' )

        if ( failures && ext.failOnTestFailures )
        {
            final message = "There were failing tests. See the report at: file:$reportFile.canonicalPath"
            if ( ext.failOnTestFailures )
            {
                throw new GradleException( message )
            }
            else
            {
                logger.warn( message )
            }
        }
    }
}
