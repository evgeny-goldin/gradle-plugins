package com.github.goldin.plugins.gradle.monitor

class MonitorExtension
{
    File                resourcesFile
    List<String>        resourcesList
    Map<String, String> headers        = [:]
    int                 connectTimeout = 30000
    int                 readTimeout    = 30000
}
