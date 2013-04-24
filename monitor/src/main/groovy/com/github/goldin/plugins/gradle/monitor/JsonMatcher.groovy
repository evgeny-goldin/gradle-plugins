package com.github.goldin.plugins.gradle.monitor

import groovy.json.JsonSlurper
import org.gcontracts.annotations.Requires


class JsonMatcher
{
    private boolean isMap  ( Object ... o ){ o.every{ it instanceof Map  }}
    private boolean isList ( Object ... o ){ o.every{ it instanceof List }}


    @Requires({ ( content != null ) && ( matcher != null ) })
    boolean contains ( String content, String matcher )
    {
        assert matcher.with { startsWith( '{' ) && endsWith( '}' ) } ||
               matcher.with { startsWith( '[' ) && endsWith( ']' ) }

        if ( ! content.with { startsWith( matcher[0] ) && endsWith( matcher[-1] ) }) { return false }

        containsGeneral( new JsonSlurper().parseText( content ),
                         new JsonSlurper().parseText( matcher ))

    }


    @Requires({ ( contentObject != null ) && ( matcherObject != null ) })
    private boolean containsGeneral ( Object contentObject, Object matcherObject )
    {
        isList( contentObject, matcherObject ) ? containsList(( List ) contentObject, ( List ) matcherObject ) :
        isMap ( contentObject, matcherObject ) ? containsMap(( Map ) contentObject, ( Map ) matcherObject ) :
                                                 contentObject.equals( matcherObject )
    }


    @Requires({ ( contentMap != null ) && ( matcherMap != null ) })
    private boolean containsMap ( Map<String,?> contentMap, Map<String,?> matcherMap )
    {
        matcherMap.every {
            String matcherKey, Object matcherValue ->
            final contentValue = contentMap[ matcherKey ]
            ( contentValue != null ) && containsGeneral( contentValue, matcherValue )
        }
    }


    @Requires({ ( contentList != null ) && ( matcherList != null ) })
    private boolean containsList ( List<?> contentList, List<?> matcherList )
    {
        matcherList.every {
            Object matcherObject ->
            isList( matcherObject ) ? contentList.any { contentObject -> isList( contentObject ) && containsList(( List ) contentObject, ( List ) matcherObject )} :
            isMap ( matcherObject ) ? contentList.any { contentObject -> isMap ( contentObject ) && containsMap(( Map ) contentObject, ( Map ) matcherObject )} :
                                      contentList.contains( matcherObject )
        }
    }
}
