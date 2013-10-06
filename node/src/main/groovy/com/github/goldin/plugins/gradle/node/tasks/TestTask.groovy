package com.github.goldin.plugins.gradle.node.tasks

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
        if ( ext.before || ext.beforeTest ) { shellExec( commandsScript( add( ext.before, ext.beforeTest )),
                                                         baseScript( 'before test' ),
                                                         scriptFileForTask( this.name, true ), false, true, false ) }

        try
        {
            runTests()
        }
        finally
        {
            if ( ext.after || ext.afterTest ) { shellExec( commandsScript( add ( ext.after, ext.afterTest )),
                                                           baseScript( 'after test' ),
                                                           scriptFileForTask( this.name, false, true ), false, true, false )}
        }
    }


    private void runTests()
    {
        final testReport = shellExec( testScript( ext.xUnitReport ), baseScript(), scriptFileForTask(), true, false )

        if ( testReport.with{ contains( '0 tests complete' ) || contains( 'no such file or directory \'test.js\'' )})
        {
            failOrWarn( ext.failIfNoTests, "No tests found:\n$testReport" )
            return
        }

        if ( ! ext.xUnitReport ) { return }

        final  teamCityReportLines = testReport.readLines()*.trim().grep().findAll { it.startsWith( '##teamcity[' )}
        assert teamCityReportLines, "Running tests produced no report in TeamCity format, use '-R teamcity' when running 'mocha'"

        final xUnitReportFile = new File( testResultsDir(), ext.xUnitReportFile )
        final failures        = writeXUnitReport( teamCityReportLines, xUnitReportFile, ext.failIfTestsFail )

        if ( failures )
        {
            failOrWarn( ext.failIfTestsFail, "There were failing tests. See the report at: file:${ xUnitReportFile.canonicalPath }" )
        }
    }


    @Requires({ ext.testCommand })
    @Ensures ({ result })
    private String testScript( boolean xUnitReport )
    {
        final isMocha     = ext.testCommand.startsWith( 'mocha' )
        final reporter    = ( xUnitReport && isMocha ) ? '-R teamcity' : ''
        final testCommand = "$ext.testCommand ${ (( ! ext.testInput ) || ( ext.testInput == 'test' )) ? '' : ext.testInput } $reporter".trim()

        """
        |echo $testCommand
        |echo
        |$testCommand${ ext.removeColorCodes }
        """.stripMargin().toString().trim()
    }


    @Ensures({ result.directory })
    private File testResultsDir()
    {
        final  testResultsDir = new File( buildDir(), 'test-results' )
        assert testResultsDir.with { directory || mkdirs() }
        testResultsDir
    }
}
