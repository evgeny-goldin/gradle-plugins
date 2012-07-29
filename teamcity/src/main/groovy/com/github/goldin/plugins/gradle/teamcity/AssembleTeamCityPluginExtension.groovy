package com.github.goldin.plugins.gradle.teamcity

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.bundling.Jar

/**
 * {@link AssembleTeamCityPluginTask} extension.
 */
@SuppressWarnings([ 'ConfusingMethodName' ])
class AssembleTeamCityPluginExtension
{
    /**
     * Template properties and setters.
     */

    String name                    = ''
    String displayName             = ''
    String version                 = ''
    String description             = ''
    String downloadUrl             = ''
    String email                   = ''
    String vendorName              = ''
    String vendorUrl               = ''
    String vendorLogo              = ''
    boolean useSeparateClassloader = false

    void name        ( String name        ){ this.name        = name        }
    void displayName ( String displayName ){ this.displayName = displayName }
    void version     ( String version     ){ this.version     = version     }
    void description ( String description ){ this.description = description }
    void downloadUrl ( String downloadUrl ){ this.downloadUrl = downloadUrl }
    void email       ( String email       ){ this.email       = email       }
    void vendorName  ( String vendorName  ){ this.vendorName  = vendorName  }
    void vendorUrl   ( String vendorUrl   ){ this.vendorUrl   = vendorUrl   }
    void vendorLogo  ( String vendorLogo  ){ this.vendorLogo  = vendorLogo  }
    void useSeparateClassloader ( boolean useSeparateClassloader ){ this.useSeparateClassloader = useSeparateClassloader }

    /**
     * Destination zip location and setter.
     */

    File destinationZip
    void destinationZip ( File destinationZip ){ this.destinationZip = destinationZip }

    /**
     * Configurations / jar tasks properties and setters.
     */

    final List<Configuration> serverConfigurations = []
    final List<Jar>           serverJars           = []
    final List<Configuration> agentConfigurations  = []
    final List<Jar>           agentJars            = []

    void serverConfigurations ( Configuration ... configurations ){ serverConfigurations.addAll( configurations )}
    void serverJars           ( Jar ... jars )                    { serverJars.addAll( jars )}
    void agentConfigurations  ( Configuration ... configurations ){ agentConfigurations. addAll( configurations )}
    void agentJars            ( Jar ... jars )                    { agentJars.addAll( jars )}
}
