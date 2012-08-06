package com.github.goldin.plugins.gradle.teamcity

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Jar


/**
 * {@link AssembleTeamCityPluginTask} extension.
 */
@SuppressWarnings([ 'ConfusingMethodName' ])
class AssembleTeamCityPluginExtension
{
    /**
     * Template properties.
     */

    String  name                         = ''
    String  displayName                  = ''
    String  version                      = ''
    String  description                  = ''
    String  downloadUrl                  = ''
    String  email                        = ''
    String  vendorName                   = ''
    String  vendorUrl                    = ''
    String  vendorLogo                   = ''
    long    minBuild                     = -1
    long    maxBuild                     = -1
    boolean useSeparateClassloader       = false
    List<Map<String, Object>> parameters = [] // Each Map has two keys: "name", "value"

    void name                   ( String name                    ){ this.name                   = name                   }
    void displayName            ( String displayName             ){ this.displayName            = displayName            }
    void version                ( String version                 ){ this.version                = version                }
    void description            ( String description             ){ this.description            = description            }
    void downloadUrl            ( String downloadUrl             ){ this.downloadUrl            = downloadUrl            }
    void email                  ( String email                   ){ this.email                  = email                  }
    void vendorName             ( String vendorName              ){ this.vendorName             = vendorName             }
    void vendorUrl              ( String vendorUrl               ){ this.vendorUrl              = vendorUrl              }
    void vendorLogo             ( String vendorLogo              ){ this.vendorLogo             = vendorLogo             }
    void minBuild               ( long   minBuild                ){ this.minBuild               = minBuild               }
    void maxBuild               ( long   maxBuild                ){ this.maxBuild               = maxBuild               }
    void parameter              ( Map<String, Object> parameter  ){ this.parameters.addAll( parameter )                  }
    void useSeparateClassloader ( boolean useSeparateClassloader ){ this.useSeparateClassloader = useSeparateClassloader }

    /**
     * Plugin / agent archive paths.
     */

    File archivePath
    void archivePath      ( File archivePath      ){ this.archivePath      = archivePath }

    File agentArchivePath
    void agentArchivePath ( File agentArchivePath ){ this.agentArchivePath = agentArchivePath }

    /**
     * Server / agent properties.
     */

    final List<Project>       serverProjects       = [] // Projects packed as 'server' plugins
    final List<Configuration> serverConfigurations = [] // 'server' plugins dependencies, 'compile' if 'serverProjects' specified
    final List<Jar>           serverJarTasks       = [] // 'jar' tasks creating 'server' plugin archives, 'jar' if 'serverProjects' specified
          File                serverResources

    final List<Project>       agentProjects        = [] // Projects packed as 'agent' plugins
    final List<Configuration> agentConfigurations  = [] // 'agent' plugins dependencies, 'compile' if 'agentProjects' specified
    final List<Jar>           agentJarTasks        = [] // 'jar' tasks creating 'agent' plugin archives, 'jar' if 'agentProjects' specified

    void server               ( Project       ... projects       ){ serverProjects.addAll( projects )}
    void serverConfigurations ( Configuration ... configurations ){ serverConfigurations.addAll( configurations )}
    void serverJarTasks       ( Jar           ... jars           ){ serverJarTasks.addAll( jars )}
    void serverResources      ( File serverResourcesDir          ){ serverResources = serverResourcesDir }

    void agent                ( Project       ... projects       ){ agentProjects.addAll( projects )}
    void agentConfigurations  ( Configuration ... configurations ){ agentConfigurations.addAll( configurations )}
    void agentJarTasks        ( Jar           ... jars           ){ agentJarTasks.addAll( jars )}

    /**
     * Static resources to be added to archive.
     * Each Map's eys: 'files' (required), 'prefix' (optional)
     */

    final List<Map<String, Object>> resources = []
    void  resources( Map<String, Object> resourcesMap ){ resources << resourcesMap }
    void  resources( FileCollection      files        ){ resources << [ files: files, prefix: '' ]}

    /**
     * Configurations to add artifact to.
     */

    final List<Configuration> artifactConfigurations = []
    void artifacts( Project ... projects ){ artifactConfigurations.addAll( projects*.configurations*.archives.flatten() )}
}
