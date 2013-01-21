#!/bin/bash

echo ---------------------------------------------
echo Running "\""setup"\"" in "\""`pwd`"\""
echo ---------------------------------------------

if [ ! -d "$HOME" ];
then
    echo "[\$HOME] is not defined"
    exit 1
fi

NVM_HOME="$HOME/.nvm"
NVM_SH="$NVM_HOME/nvm.sh"

if [ -f "$NVM_SH" ]; then
    . "$NVM_SH"
    nvm ls
fi

if [ ! -f "$NVM_SH" ] || [ $? -ne 0 ]; then
    rm -rf "$NVM_HOME"
    git clone ${nvmRepo} "$NVM_HOME"
fi

if [ ! -f "$NVM_SH" ];
then
    echo "[$NVM_SH] not found"
    exit 1
fi

. "$NVM_SH"

nvm install ${nodeVersion}
nvm use     ${nodeVersion}

echo "npm  : [`which npm`][`npm --version`]"
echo "node : [`which node`][`node --version`]"

echo npm install
npm install
