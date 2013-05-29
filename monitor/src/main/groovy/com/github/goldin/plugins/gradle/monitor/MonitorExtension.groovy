package com.github.goldin.plugins.gradle.monitor

class MonitorExtension
{
    String              user
    String              password
    File                resourcesFile
    List<String>        resourcesList
    String              matchersDelimiter = '*'
    Map<String, String> headers           = [:]
    int                 connectTimeout    = 30000
    int                 readTimeout       = 30000
    boolean             runInParallel     = true
    int                 plotBuilds        = 3
    File                plotJsonFile      = new File( 'plot-data.json' )
    File                plotFile
}
