~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Gradle plugins, see "examples" folder.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


["gradle-about-plugin"](http://evgeny-goldin.com/wiki/Gradle-about-plugin)
========================================================================================

    apply plugin: 'about'

    ..

    buildscript {
        repositories { mavenRepo url: 'http://evgeny-goldin.org/artifactory/repo/' }
        dependencies { classpath      'com.goldin.plugins:gradle:0.1.1' }
    }

    ..

    about {
        dumpSCM          = true // To dump project's SCM details,        true  by default
        dumpDependencies = true // To dump project's dependencies,       false by default
        dumpEnv          = true // To dump system environment variables, false by default
        dumpSystem       = true // To dump Java system properties,       false by default
        dumpPaths        = true // To dump system paths,                 false by default
    }

    assemble.doLast { about.execute() }
    install.doFirst { about.execute() }


Or run `gradle build about`.
See [examples/about-dump-dependencies](https://github.com/evgeny-goldin/gradle-plugins/tree/master/examples/about-dump-dependencies) or [real usage](https://github.com/evgeny-goldin/gcommons/blob/master/build.gradle).


["gradle-duplicates-plugin"](http://evgeny-goldin.com/wiki/Gradle-duplicates-plugin)
========================================================================================

    apply plugin: 'groovy'
    apply plugin: 'duplicates'

    buildscript {
        repositories { mavenRepo url: 'http://evgeny-goldin.org/artifactory/repo/' }
        dependencies { classpath      'com.goldin.plugins:gradle:0.1.1' }
    }

    repositories { mavenRepo urls: 'http://evgeny-goldin.org/artifactory/repo/' }
    duplicates   { configurations = [ 'compile', 'testCompile' ],
                   verbose        = true }

    ..


Now run `gradle duplicates`.
See [examples/duplicate-no-verbose](https://github.com/evgeny-goldin/gradle-plugins/tree/master/examples/duplicate-no-verbose), [examples/duplicate-verbose](https://github.com/evgeny-goldin/gradle-plugins/tree/master/examples/duplicate-verbose) or [real usage](https://github.com/evgeny-goldin/gcommons/blob/master/build.gradle).


["gradle-codenarc-plugin"](http://evgeny-goldin.com/wiki/Gradle-CodeNarc-plugin)
========================================================================================

    codenarcVersion             = '0.16.1'
    codenarcPriority3Violations = 1
    apply from: 'https://raw.github.com/evgeny-goldin/gradle-plugins/master/src/main/groovy/CodeNarc.gradle'


Now run `gradle codenarc`.
See [examples/codenarc](https://github.com/evgeny-goldin/gradle-plugins/tree/master/examples/codenarc) or [real usage](https://github.com/evgeny-goldin/teamcity-plugins/blob/master/build.gradle).
