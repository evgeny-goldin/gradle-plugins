package com.github.goldin.plugins.gradle.common.helpers

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import java.util.regex.Pattern


final class MatcherHelper extends BaseHelper<Object>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    MatcherHelper(){ super( null, null, null )}


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    MatcherHelper ( Project project, BaseTask task, Object ext ) { super( project, task, ext )}


    private boolean isMap  ( Object ... o ){ o.every{ it instanceof Map  }}
    private boolean isList ( Object ... o ){ o.every{ it instanceof List }}


    /**
     * Determines if content provided contains patterns specified.
     */
    @SuppressWarnings([ 'GroovyAssignmentToForLoopParameter' ])
    @Requires({ ( content != null ) && ( patterns != null ) })
    boolean contentMatches( String content, String patterns, String matchersDelimiter )
    {
        final matchersList = matchersDelimiter ? patterns.tokenize( matchersDelimiter ) : [ patterns ]
        for ( matcher in matchersList*.trim())
        {
            final positiveMatch = ( ! matcher.startsWith( '-' ))
            matcher             = positiveMatch ? matcher : matcher[ 1 .. -1 ]
            final regexMatch    = matcher.with { startsWith( '/' ) && endsWith( '/' ) }
            final jsonMatch     = matcher.with { startsWith( '{' ) && endsWith( '}' ) } ||
                                  matcher.with { startsWith( '[' ) && endsWith( ']' ) }
            matcher             = regexMatch    ? matcher[ 1 .. -2 ] : matcher
            final isMatch       = regexMatch    ? Pattern.compile ( matcher ).matcher( content ).find() :
                                  jsonMatch     ? jsonContains( content, matcher ) :
                                                  content.contains( matcher )

            if ( positiveMatch ? ( ! isMatch ) : isMatch ) { return false }
        }

        true
    }


    @Requires({ ( content != null ) && ( matcher != null ) })
    private boolean jsonContains ( String content, String matcher )
    {
        assert matcher.with { startsWith( '{' ) && endsWith( '}' ) } ||
               matcher.with { startsWith( '[' ) && endsWith( ']' ) }

        if ( ! content.with { startsWith( matcher[0] ) && endsWith( matcher[-1] ) }) { return false }

        final helper = new JsonHelper()
        jsonContainsGeneral( helper.jsonToObject( content, Object ),
                             helper.jsonToObject( matcher, Object ))
    }


    @Requires({ ( contentObject != null ) && ( matcherObject != null ) })
    private boolean jsonContainsGeneral ( Object contentObject, Object matcherObject )
    {
        isList( contentObject, matcherObject ) ? jsonContainsList(( List ) contentObject, ( List ) matcherObject ) :
        isMap ( contentObject, matcherObject ) ? jsonContainsMap(( Map ) contentObject, ( Map ) matcherObject ) :
                                                 contentObject.equals( matcherObject )
    }


    @Requires({ ( contentMap != null ) && ( matcherMap != null ) })
    private boolean jsonContainsMap ( Map<String,?> contentMap, Map<String,?> matcherMap )
    {
        matcherMap.every {
            String matcherKey, Object matcherValue ->
            final contentValue = contentMap[ matcherKey ]
            ( contentValue != null ) && jsonContainsGeneral( contentValue, matcherValue )
        }
    }


    @Requires({ ( contentList != null ) && ( matcherList != null ) })
    private boolean jsonContainsList ( List<?> contentList, List<?> matcherList )
    {
        matcherList.every {
            Object matcherObject ->
            isList( matcherObject ) ? contentList.any { contentObject -> isList( contentObject ) && jsonContainsList(( List ) contentObject, ( List ) matcherObject )} :
            isMap ( matcherObject ) ? contentList.any { contentObject -> isMap ( contentObject ) && jsonContainsMap(( Map ) contentObject, ( Map ) matcherObject )} :
                                      contentList.contains( matcherObject )
        }
    }
}
