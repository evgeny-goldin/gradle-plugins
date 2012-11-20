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
    git clone git://github.com/creationix/nvm.git "$NVM_HOME"
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
nvm use     ${nodeVersion}

export NODE_ENV=${NODE_ENV}

echo "Current 'npm'     : [`which npm`][`npm --version`]"
echo "Current 'node'    : [`which node`][`node --version`]"
echo "Current \$NODE_ENV : [$NODE_ENV]"
echo

echo "Running 'npm install'"
npm install
