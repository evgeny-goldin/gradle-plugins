~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Gradle plugins, see "examples" folder.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To use "gradle-about-plugin" ( http://evgeny-goldin.com/wiki/Gradle-about-plugin ):

---------------------------------
apply plugin: 'about'

..

buildscript {
    repositories { mavenRepo urls: 'http://evgeny-goldin.org/artifactory/repo/' }
    dependencies { classpath 'com.goldin.plugins:gradle:0.1-RC2'                }
}

..

about {
    dumpSCM          = true // To dump project's SCM details,        true  by default
    dumpDependencies = true // To dump project's dependencies,       false by default
    dumpEnv          = true // To dump system environment variables, false by default
    dumpSystem       = true // To dump Java system properties,       false by default
    dumpPaths        = true // To dump system paths,                 false by default
}

assemble.doLast { about.execute() } // Or simply run
install.doFirst { about.execute() } // "> gradle build about"
---------------------------------

To use "gradle-duplicates-plugin" ( http://evgeny-goldin.com/wiki/Gradle-duplicates-plugin ):

---------------------------------
apply plugin: 'groovy'
apply plugin: 'duplicates'

buildscript {
    repositories { mavenRepo urls: 'http://evgeny-goldin.org/artifactory/repo/' }
    dependencies { classpath 'com.goldin.plugins:gradle:0.1-RC2' }
}

repositories { mavenRepo urls: 'http://evgeny-goldin.org/artifactory/repo/' }
duplicates   { configurations = [ 'compile', 'testCompile' ],
               verbose        = true }

..
---------------------------------

Run "gradle duplicates".
