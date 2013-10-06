package com.github.goldin.plugins.gradle.play

import com.github.goldin.plugins.gradle.common.extensions.ShellExtension


@SuppressWarnings([ 'UnusedVariable' ])
class PlayExtension extends ShellExtension
{
    String  appName
    String  playVersion     = '2.2.0'
    String  playHome        = '.play'

    int     port            = 9000 // Port the application starts on
    String  address         = '0.0.0.0'
    String  config          = 'conf/application.conf'
    String  arguments       = ''

    int     debugPort       = 8000
    boolean debug           = false

    List<Map<String,?>> js  = []
    List<Map<String,?>> css = []

    /**
     * Internal properties
     */

    boolean updated       = false
    String  playArguments = ''
    String  playZip
    String  playUrl
    String  playDirectory
    String  play
}
