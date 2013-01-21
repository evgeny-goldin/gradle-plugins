package com.github.goldin.plugins.gradle.monitor

class MonitorExtension
{
    File                resources
    Map<String, String> headers        = [:]
    int                 connectTimeout = 30000
    int                 readTimeout    = 30000
}
