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
    final Properties teamcityProperties = ( Properties ) systemProperties[ 'teamcity' ]
    final String     teamCityUrl        = systemEnv.TEAMCITY_URL?.replaceAll( /(?<!\\|\/)(\\|\/)*$/, '/' ) ?: '' // Eliminates extra tail slashes
    final String     teamCityBuildId    = ( teamcityProperties ? teamcityProperties[ 'teamcity.build.id' ] : '' )
    final String     teamCityBuildUrl   = ( teamCityUrl && teamCityBuildId ? "${teamCityUrl}viewLog.html?buildId=$teamCityBuildId" : '' )


    @Requires({ propertyName })
    @Ensures ({ result != null })
    String teamcityProperty( String propertyName ){ ( teamcityProperties ? ( teamcityProperties[ ( propertyName ) ] ?: '' ) : '' ) }


    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    TeamCityHelper ( Project project, BaseTask task, Object ext )
    {
        super( project, task, ext )
        assert (( ! teamCityUrl ) || teamCityUrl.endsWith( '/' ))
        log { "Properties: $teamcityProperties, url: [$teamCityUrl], buildId: [$teamCityBuildId], buildUrl: [$teamCityBuildUrl]" }
    }
}
