package com.github.goldin.plugins.gradle.node.tasks

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test


/**
 * Tests Node.js application.
 */
class NodeTestTask extends NodeBaseTask
{

    @Override
    void taskAction()
    {
        final testReport = bashExec( testScript(), scriptPath( TEST_SCRIPT ), false, ext.generateOnly )

        if ( ext.generateOnly ) { return }

        final teamCityReport  = testReport.readLines()*.trim().grep().findAll { it.startsWith( '##teamcity[' )}
        final xUnitReportFile = new File( testResultsDir(), 'TEST-node.xml' )
        final failures        = writeXUnitReport( teamCityReport, xUnitReportFile )

        if ( failures )
        {
            final message = "There were failing tests. See the report at: file:$xUnitReportFile.canonicalPath"
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
        final  testResultsDir = ( testTask instanceof Test ) ? (( Test ) testTask ).testResultsDir :
                                                               new File( 'build/test-results' )
        assert testResultsDir.with { directory || mkdirs() }
        testResultsDir
    }


    /**
     * Converts TeamCity test report into xUnit report and writes into a file specified.
     *
     * @param teamCityReport TeamCity test report lines
     * @param reportFile     file to write xUnit report to
     * @return               true if any failures were found, false otherwise
     */
    @Requires({ teamCityReport && reportFile })
    private boolean writeXUnitReport ( List<String> teamCityReport, File reportFile )
    {
        assert teamCityReport.every { it.startsWith( '##teamcity[' )}
        assert teamCityReport[  0 ].startsWith     ( '##teamcity[testSuiteStarted name=\'' )
        assert teamCityReport[ -1 ].startsWith     ( '##teamcity[testSuiteFinished name=\'' )

        final reportLines = []
        int   tests       = 0
        int   failures    = 0
        int   skipped     = 0

        for ( line in teamCityReport[ 1 .. -2 ] )
        {
            final attributes = lineToMap( line )
            final testName   = attribute( attributes, 'name', line )

            if ( line.startsWith( '##teamcity[testFinished ' ))
            {
                tests++
                final duration = attribute( attributes, 'duration', line, { String s -> ( s ==~ NumberPattern ? ( s as int ) / 1000 /* ms => sec */ : -1 )})
                reportLines << """<testcase name="$testName" time="$duration"/>"""
            }
            else if ( line.startsWith( '##teamcity[testFailed ' ))
            {
                failures++
                reportLines << """<testcase name="$testName"><failure message="${ attribute( attributes, 'message', line ) }"/></testcase>"""
            }
            else if ( line.startsWith( '##teamcity[testIgnored ' ))
            {
                skipped++
                reportLines << """<testcase name="$testName"><skipped/></testcase>"""
            }
        }

        final testSuiteName      = attribute( lineToMap( teamCityReport[ 0 ] ), 'name', teamCityReport[ 0 ])
        final String xUnitReport = """
<testsuite name="$testSuiteName" tests="$tests" failures="$failures" skip="$skipped">
    ${ reportLines.join( '\n    ' ) }
</testsuite>""".trim()

        assert reportFile.with { ( ! file ) || ( project.delete( delegate )) }
        reportFile.write( xUnitReport, 'UTF-8' )

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
     * Converts a line into a {@code Map} of key => value of its attributes.
     * @param line line to read,
     *        example: {@code "##teamcity[testFailed name='RTB - Campaign "before all" hook' message='ER_ACCESS_DENIED_ERROR: Access denied for user |'root|'@|'localhost|' (using password: NO)']"}
     * @return {@code Map} of key => value of its attributes.
     */
    @Requires({ line })
    @Ensures ({ result != null })
    private Map<String, String> lineToMap( String line )
    {
        line.findAll( AttributePattern ){ it [ 1 .. 2 ] }.inject( [:] ){
            Map m, List<String> l -> m[ l[ 0 ] ] = l[ 1 ].trim().
                                                          replace( "|'", "'"    ).
                                                          replace( '"',  "'"    ).
                                                          replace( '<',  '&lt;' ).
                                                          replace( '>',  '&gt;' )
            m
        }
    }
}
