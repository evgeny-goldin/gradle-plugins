#!/bin/bash

echo ---------------------------------------------
echo Running "\""setup"\"" in "\""`pwd`"\""
echo ---------------------------------------------
currentDir=`pwd`

if [ ! -d "$HOME" ];
then
    echo "[\$HOME] is not defined"
    exit 1
fi

NVM_HOME="$HOME/.nvm"
NVM_SH="$NVM_HOME/nvm.sh"

. "$NVM_SH"
nvm ls

if [ $? -ne 0 ] || [ ! -f "$NVM_SH" ]; then
    rm -rf "$NVM_HOME"
    git clone    ${nvmRepo} "$NVM_HOME"
    cd  "$NVM_HOME"
    git checkout ${nvmCommit}
    cd  "$currentDir"
fi

if [ ! -f "$NVM_SH" ];
then
    echo "[$NVM_SH] not found"
    exit 1
fi

set -e
set -o pipefail

. "$NVM_SH"

nvm install ${nodeVersion}
nvm use     ${nodeVersion}

echo "npm  : [`which npm`][`npm --version`]"
echo "node : [`which node`][`node --version`]"

echo npm install
npm install
