package com.github.goldin.plugins.gradle.play

import com.github.goldin.plugins.gradle.common.node.NodeBaseExtension


@SuppressWarnings([ 'UnusedVariable' ])
class PlayExtension extends NodeBaseExtension
{
    String  appName
    String  playHome             = '.play'

    int     port                 = 9000 // Port the application starts on
    String  address              = '0.0.0.0'
    String  config               = 'conf/application.conf'
    String  arguments            = ''

    List<Map<String,?>> grunt    = []

    Map<String,String>  versions        = [:]
    Map<String,String>  defaultVersions = [ 'play'                 : '2.2.0',
                                            'node'                 : '0.10.20',
                                            'grunt'                : '0.4.1',
                                            'grunt-cli'            : '0.1.9',
                                            'coffee-script'        : '1.6.3',
                                            'uglify-js'            : '2.4.0',
                                            'grunt-contrib-clean'  : '0.5.0',
                                            'grunt-contrib-coffee' : '0.7.0',
                                            'grunt-contrib-uglify' : '0.2.4',
                                            'grunt-contrib-less'   : '0.7.0' ]
    /**
     * Internal properties
     */

    String  playVersion
    String  playArguments = ''
    String  playZip
    String  playUrl
    String  playDirectory
    String  play
}
