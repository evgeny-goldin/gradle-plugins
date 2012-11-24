#!/bin/bash

if [ ! -d "$HOME" ];
then
    echo "[\$HOME] is not defined"
    exit 1
fi

NVM_HOME="$HOME/.nvm"
. "$NVM_HOME/nvm.sh"
nvm ls

if [ $? -ne 0 ]; then
    git clone ${nvmRepo} "$NVM_HOME"
fi

if [ ! -f "$NVM_HOME/nvm.sh" ];
then
    echo "[$NVM_HOME/nvm.sh] not found"
    exit 1
fi

set -e
set -o pipefail

. "$NVM_HOME/nvm.sh"

nvm install ${nodeVersion}
${nvmAlias}
nvm use     ${nodeVersion}
npm install ${globally}

echo "npm  : [`which npm`][`npm --version`]"
echo "node : [`which node`][`node --version`]"
