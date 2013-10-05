package com.github.goldin.plugins.gradle.play

import com.github.goldin.plugins.gradle.common.extensions.BaseShellExtension


@SuppressWarnings([ 'UnusedVariable' ])
class PlayExtension extends BaseShellExtension
{
    String  appName
    String  playVersion     = '2.2.0'
    String  playHome        = '.play'

    int     port            = 9000
    String  address         = '0.0.0.0'
    String  config          = 'conf/application.conf'
    String  arguments       = ''

    boolean stopBeforeStart = true  // Whether 'stop' should run before 'start'

    int     debugPort       = 8000
    boolean debug           = false

    List<Map<String,?>> js  = []
    List<Map<String,?>> css = []

    /**
     * Internal properties
     */

    boolean updated = false
    String  playZip
    String  playUrl
    String  playDirectory
    String  play
}
