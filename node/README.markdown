Node.js Gradle plugin
----------------------

Tasks provided by the plugin:

* `clean`        - deletes all files generated: bash scripts and test results
* `cleanModules` - deletes all locally installed Node.js modules
* `setup`        - sets up the environment and installs Node.js modules required
* `test`         - runs tests, dependsOn `setup` - use `-x setup` to skip
* `stop`         - stops an application, dependsOn `setup` - use `-x setup` to skip
* `start`        - starts an application, dependsOn `setup` - use `-x setup` to skip


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
