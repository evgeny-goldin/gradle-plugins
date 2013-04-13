package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Tests Node.js application.
 */
class TestTask extends NodeBaseTask
{
    @Override
    void taskAction()
    {
        if ( ext.before ) { bashExec( commandsScript( ext.before ), taskScriptFile( true ), false, true, true, false, 'before test' ) }

        try
        {
            runTests()
        }
        finally
        {
            if ( ext.after ) { bashExec( commandsScript( ext.after ), taskScriptFile( false, true ), false, true, true, false, 'after test' )}
        }
    }


    private void runTests ()
    {
        final testReport = bashExec( testScript( ext.xUnitReport ? '-R teamcity' : '' ), taskScriptFile(), true, true, false )

        if ( testReport.contains( 'no such file or directory \'test.js\'' ))
        {
            log( LogLevel.WARN ){ "No 'mocha' tests found" }
            return
        }

        if ( ! ext.xUnitReport ) { return }

        final teamCityReportLines = testReport.readLines()*.trim().grep().findAll { it.startsWith( '##teamcity[' )}
        if ( ! teamCityReportLines ) { throw new GradleException( "Running tests produced no test report:\n$testReport" )}

        final xUnitReportFile = new File( testResultsDir(), ext.xUnitReportFile )
        final failures        = writeXUnitReport( teamCityReportLines, xUnitReportFile )

        if ( failures )
        {
            failOrWarn( ext.failIfTestsFail, "There were failing tests. See the report at: file:${ xUnitReportFile.canonicalPath }" )
        }
    }

    @Requires({ testArguments != null })
    @Ensures ({ result })
    private String testScript( String testArguments )
    {
        assert ext.testCommand.startsWith( 'mocha' ), "Only 'mocha' test runner is currently supported"
        final testCommand = "$ext.testCommand ${ (( ! ext.testInput ) || ( ext.testInput == 'test' )) ? '' : ext.testInput } $testArguments".trim()

        """
        |echo $testCommand
        |echo
        |$testCommand
        """.stripMargin().toString().trim()
    }


    @Ensures({ result.directory })
    private File testResultsDir()
    {
        final  testResultsDir = new File( buildDir(), 'test-results' )
        assert testResultsDir.with { directory || mkdirs() }
        testResultsDir
    }


    /**
     * Converts TeamCity test report into xUnit report and writes into a file specified.
     *
     * @param teamCityReportLines TeamCity test report lines
     * @param reportFile          file to write xUnit report to
     * @return                    true if any failures were found, false otherwise
     */
    @Requires({ teamCityReportLines && reportFile })
    private boolean writeXUnitReport ( List<String> teamCityReportLines, File reportFile )
    {
        assert teamCityReportLines.every { it.startsWith( '##teamcity[' )}
        assert teamCityReportLines[  0 ].startsWith     ( '##teamcity[testSuiteStarted name=\'' )
        assert teamCityReportLines[ -1 ].startsWith     ( '##teamcity[testSuiteFinished name=\'' )

        final xUnitReportLines = []
        int   tests            = 0
        int   failures         = 0
        int   skipped          = 0

        for ( line in teamCityReportLines[ 1 .. -2 ] )
        {
            final attributes = teamCityReportLineToMap( line )
            final testName   = attribute( attributes, 'name', line )

            if ( line.startsWith( '##teamcity[testFinished ' ))
            {
                tests++
                final duration = attribute( attributes, 'duration', line, { String s -> ( s ==~ NumberPattern ? ( s as int ) / 1000 /* ms => sec */ : -1 )})
                xUnitReportLines << """<testcase name="$testName" time="$duration"/>"""
            }
            else if ( line.startsWith( '##teamcity[testFailed ' ))
            {
                failures++
                xUnitReportLines << """<testcase name="$testName"><failure message="${ attribute( attributes, 'message', line ) }"/></testcase>"""
            }
            else if ( line.startsWith( '##teamcity[testIgnored ' ))
            {
                skipped++
                xUnitReportLines << """<testcase name="$testName"><skipped/></testcase>"""
            }
        }

        final testSuiteName      = attribute( teamCityReportLineToMap( teamCityReportLines[ 0 ] ), 'name', teamCityReportLines[ 0 ])
        final String xUnitReport = """
<testsuite name="$testSuiteName" tests="$tests" failures="$failures" skip="$skipped">
    ${ xUnitReportLines.join( '\n    ' ) }
</testsuite>""".trim()

        write( reportFile, xUnitReport )
        log{ "xUnit report is created at: file:${ reportFile.canonicalPath }" }

        ( failures > 0 )
    }


    /**
     * Reads an attribute from a map of attributes.
     *
     * @param attributes map of attributes to read
     * @param attributeName name of an attribute to read
     * @param originalLine line where map of attributes came from
     * @param transform attribute value transformation, optional
     * @return attribute value, possible transformed by {@code transform} closure
     */
    @Requires({ attributes && attributeName && originalLine })
    @Ensures ({ result != null })
    private Object attribute( Map<String, String> attributes,
                              String              attributeName,
                              String              originalLine,
                              Closure             transform = null )
    {
        final    attributeValue = attributes[ attributeName ]
        assert ( attributeValue != null ), "Line [$originalLine] contains no required attribute [$attributeName]"

        transform ? transform( attributeValue ) : attributeValue
    }


    /**
     * Converts a TeamCity report line into a {@code Map} of key => value of line's attributes.
     * @param line line to read,
     *        example: {@code "##teamcity[testFailed name='RTB - Campaign "before all" hook' message='ER_ACCESS_DENIED_ERROR: Access denied for user |'root|'@|'localhost|' (using password: NO)']"}
     * @return {@code Map} of key => value of line's attributes.
     */
    @Requires({ line })
    @Ensures ({ result != null })
    private Map<String, String> teamCityReportLineToMap ( String line )
    {   // http://confluence.jetbrains.net/display/TCD7/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-ServiceMessages
        ( line.findAll( AttributePattern,      { it [ 1 .. 2 ] } ) +
          line.findAll( EmptyAttributePattern, { it [ 1 .. 2 ] } )).inject( [:] ){
            Map m, List<String> l -> // l[ 0 ] is attribute name, l[ 1 ] is attribute value
                m[ l[ 0 ] ] = l[ 1 ].trim().
                                     replace( "|'", "'"     ).
                                     replace( '"',  "'"     ).
                                     replace( '&',  '&amp;' ).
                                     replace( '<',  '&lt;'  ).
                                     replace( '>',  '&gt;'  )
            m
        }
    }
}
