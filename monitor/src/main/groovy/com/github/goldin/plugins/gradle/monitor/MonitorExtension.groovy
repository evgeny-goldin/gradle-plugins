package com.github.goldin.plugins.gradle.monitor

import com.github.goldin.plugins.gradle.common.extensions.BaseExtension

class MonitorExtension extends BaseExtension
{
    Object              resources
    String              user
    String              password
    String              matchersDelimiter = '*'
    Map<String, String> headers           = [:]
    int                 connectTimeout    = 30000
    int                 readTimeout       = 30000
    boolean             runInParallel     = true
    int                 plotBuilds        = 3
    File                plotJsonFile      = new File( 'plot-data.json' )
    File                plotFile
}
