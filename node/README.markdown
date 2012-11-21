Node.js Gradle plugin
----------------------

Tasks provided by the plugin:

* `clean` (or `nodeClean` if `clean` already exists) - removes all locally installed Node.js modules and all bash scripts generated
* `setup` - sets up the environment and installs all required modules
* `test` (or `nodeTest`) - runs tests using Mocha
* `start` - starts an application using `forever`


Sample `build.gradle`:

    apply plugin: 'node'

    defaultTasks 'test'

    buildscript {
        repositories { maven { url 'http://evgenyg.artifactoryonline.com/evgenyg/repo/' }}
        dependencies { classpath   'com.github.goldin.plugins.gradle:node:0.2-SNAPSHOT' }
    }

    node {
        NODE_ENV           = 'stage'
        failOnTestFailures = false
        startCommand       = 'forever start ./node_modules/.bin/coffee server.coffee'
        configs            = [
            [ "config/${NODE_ENV}.json" : file( "${ System.getProperty( 'user.home' )}/config.json" )],
            [ "config/${NODE_ENV}.json" : [ port :  System.getProperty( 'port', '3010' ) as int ]],
        ]
    }


    task wrapper( type: Wrapper ) { gradleVersion = '1.2' }

The plugin will update `config/stage.json` twice:

* Using `config.json` in user home directory
* Using `port` system property or a default 3010 port
