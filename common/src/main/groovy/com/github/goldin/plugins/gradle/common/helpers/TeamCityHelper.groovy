package com.github.goldin.plugins.gradle.common.helpers

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Helper class providing an access to TeamCity related data.
 * http://confluence.jetbrains.net/display/TCD7/Predefined+Build+Parameters
 */
@SuppressWarnings([ 'AbstractClassWithoutAbstractMethod' ])
class TeamCityHelper extends BaseHelper<Object>
{
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

        log { "TeamCity properties: $teamcityProperties" }
        log { "TeamCity URL       : [$teamCityUrl]"      }
        log { "TeamCity build URL : [$teamCityBuildUrl]" }
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
}
