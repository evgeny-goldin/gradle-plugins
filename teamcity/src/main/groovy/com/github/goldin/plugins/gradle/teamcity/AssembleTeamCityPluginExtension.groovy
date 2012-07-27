package com.github.goldin.plugins.gradle.teamcity


/**
 * {@link AssembleTeamCityPluginTask} extension.
 */
class AssembleTeamCityPluginExtension
{
    String displayName    = ''
    String description    = ''
    String vendorName     = ''
    String vendorUrl      = ''
    File   destinationZip = null
    String type           = 'server'
}
