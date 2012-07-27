package com.github.goldin.plugins.gradle.teamcity

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.bundling.Jar


/**
 * {@link AssembleTeamCityPluginTask} extension.
 */
@SuppressWarnings([ 'ConfusingMethodName' ])
class AssembleTeamCityPluginExtension
{
    String displayName = ''
    String description = ''
    String vendorName  = ''
    String vendorUrl   = ''
    File   destinationZip

    final List<Configuration> serverConfigurations = []
    final List<Jar>           serverJars           = []
    final List<Configuration> agentConfigurations  = []
    final List<Jar>           agentJars            = []

    /**
     * Methods invoked by assembleTeamcityPluginConfig { .. } configuration closure.
     */

    void displayName    ( String displayName    ){ this.displayName    = displayName    }
    void description    ( String description    ){ this.description    = description    }
    void vendorName     ( String vendorName     ){ this.vendorName     = vendorName     }
    void vendorUrl      ( String vendorUrl      ){ this.vendorUrl      = vendorUrl      }
    void destinationZip ( File   destinationZip ){ this.destinationZip = destinationZip }

    void serverConfigurations( Configuration ... configurations ){ serverConfigurations.addAll( configurations )}
    void serverJars          ( Jar ... jars ){ serverJars.addAll( jars )}

    void agentConfigurations ( Configuration ... configurations ){ agentConfigurations. addAll( configurations )}
    void agentJars           ( Jar ... jars ){ agentJars.addAll( jars )}
}
