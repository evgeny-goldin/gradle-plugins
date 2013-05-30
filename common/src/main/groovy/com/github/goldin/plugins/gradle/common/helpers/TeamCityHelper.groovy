package com.github.goldin.plugins.gradle.common.helpers

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import java.util.regex.Pattern


/**
 * Helper class providing an access to TeamCity related data.
 * http://confluence.jetbrains.net/display/TCD7/Predefined+Build+Parameters
 */
@SuppressWarnings([ 'AbstractClassWithoutAbstractMethod' ])
class TeamCityHelper extends BaseHelper<Object>
{
    private final static Pattern AttributePattern      = ~/(\w+)='(.*?[^|])'/
    private final static Pattern EmptyAttributePattern = ~/(\w+)='()'/
    private final static Pattern NumberPattern         = ~/^\d+$/


    final Map<String,?> teamcityProperties = readTeamcityProperties()?.asImmutable()
    final String        teamCityUrl        = systemEnv.TEAMCITY_URL?.replaceAll( /(?<!\\|\/)(\\|\/)*$/, '/' ) ?: '' // Leaves a single slash at the end of a URL
    final String        teamCityBuildUrl   = teamcityProperty( 'teamcity.build.id' ).with {
        String buildId -> ( teamCityUrl && buildId ) ? "${teamCityUrl}viewLog.html?buildId=$buildId" : ''
    }


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    TeamCityHelper ( Project project, BaseTask task, Object ext )
    {
        super( project, task, ext )
        assert (( ! teamCityUrl ) || teamCityUrl.endsWith( '/' ))

        log( LogLevel.DEBUG ) { "TeamCity properties: $teamcityProperties" }
        log( LogLevel.DEBUG ) { "TeamCity URL       : [$teamCityUrl]"      }
        log( LogLevel.DEBUG ) { "TeamCity build URL : [$teamCityBuildUrl]" }
    }


    @Requires({ propertyName })
    @Ensures ({ result != null })
    String teamcityProperty( String propertyName ){ teamcityProperties?.get( propertyName ) ?: '' }


    Map<String,?> readTeamcityProperties()
    {
        final propertiesPath = systemEnv.TEAMCITY_BUILD_PROPERTIES_FILE

        if ( propertiesPath )
        {
            final propertiesFile = new File( propertiesPath )
            if ( propertiesFile.file )
            {
                final  p = new Properties()
                propertiesFile.withReader { Reader r -> p.load( r )}
                return ( Map<String,?> ) p
            }
        }

        null
    }



    /**
     * Converts TeamCity test report into xUnit report and writes into a file specified.
     *
     * @param teamCityReportLines TeamCity test report lines
     * @param reportFile          file to write xUnit report to
     * @return                    true if any failures were found, false otherwise
     */
    @Requires({ teamCityReportLines && reportFile })
    boolean writeXUnitReport ( List<String> teamCityReportLines, File reportFile, boolean failIfTestsFail )
    {
        assert teamCityReportLines.every { it.startsWith( '##teamcity[' )}
        final boolean reportCompleted = teamCityReportLines[  0 ].startsWith( '##teamcity[testSuiteStarted ' ) &&
                                        teamCityReportLines[ -1 ].startsWith( '##teamcity[testSuiteFinished ' )
        if ( ! reportCompleted )
        {
            failOrWarn( failIfTestsFail, "TeamCity test report is incomplete:\n${ teamCityReportLines.join( '\n' )}" )
            return false
        }

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
     * @param attributes    map of attributes to read
     * @param attributeName name of an attribute to read
     * @param originalLine  line where map of attributes came from
     * @param transform     attribute value transformation, optional
     * @return attribute value, possible transformed by {@code transform} closure
     */
    @Requires({ attributes && attributeName && originalLine })
    @Ensures ({ result != null })
    Object attribute( Map<String, String> attributes,
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
    Map<String, String> teamCityReportLineToMap ( String line )
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
