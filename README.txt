~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Gradle plugins
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 -=-= "gradle-about-plugin"=-=-
---------------------------------
apply plugin: 'about'

..

buildscript {
    repositories { mavenRepo urls: 'http://evgeny-goldin.org/artifactory/repo/' }
    dependencies { classpath       'com.goldin.plugins:gradle:0.1-RC' }
}

..

about  { dumpDependencies = true }
jar << { about.execute() }
---------------------------------
Or run

> gradle clean build about
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
