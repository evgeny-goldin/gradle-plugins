package com.github.goldin.plugins.gradle.play

import com.github.goldin.plugins.gradle.common.extensions.BaseShellExtension


@SuppressWarnings([ 'UnusedVariable' ])
class PlayExtension extends BaseShellExtension
{
    String  playVersion     = '2.2.0'
    String  playUrl         = "http://downloads.typesafe.com/play/${playVersion}/play-${playVersion}.zip"

    int     port            = 9000
    String  address         = '0.0.0.0'
    String  conf            = 'conf/application.conf'
    String  arguments       = ''

    int     debugPort       = 8000
    boolean debug           = false

    List<Map<String,?>> js  = []
    List<Map<String,?>> css = []
}
