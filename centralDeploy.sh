#!/bin/bash

set -e
set -o pipefail

# Requires:
# 1) ~/.gnupg
# 2) 'gpgPassphrase' environment variable
# 3) 'sonatype-nexus-snapshots'/'sonatype-nexus-staging' credentials in "maven/conf/settings.xml"

version=`cat gradle.properties | grep 'version' | cut -d '=' -f 2`
command="mvn -B -e org.apache.maven.plugins:maven-gpg-plugin:1.4:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -Dgpg.passphrase=$gpgPassphrase"
rootDir=`pwd`

for module in 'about' 'common' 'crawler' 'duplicates' 'gitdump' 'monitor' 'node' 'teamcity'
do
    cd "$rootDir/$module"
    echo "[<===[`pwd`]===>]"
    ls -al build/pom.xml build/libs/$module-$version.jar build/libs/$module-$version-javadoc.jar build/libs/$module-$version-sources.jar
    $command -DpomFile=build/pom.xml -Dfile=build/libs/$module-$version.jar -Dfiles=build/libs/$module-$version-javadoc.jar,build/libs/$module-$version-sources.jar -Dclassifiers=javadoc,sources -Dtypes=jar,jar
done
