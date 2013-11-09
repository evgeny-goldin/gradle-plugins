#!@{shell}

currentDir=`pwd`
sudoCommand=""

echo @{LOG_DELIMITER}
echo "Executing "\""setup"\"" task in "\""$currentDir"\"""
echo "Running   @{SCRIPT_LOCATION}"
echo @{LOG_DELIMITER}

if [ ! -d "$HOME" ];
then
    echo "[\$HOME] is not defined"
    exit 1
fi

if [ "`sudo -n chown 2>&1 | grep "sorry" | wc -l | awk '{print $1}'`" == "0" ];
then
    # Good, 'sudo chown' may run without asking for a password
    sudoCommand="sudo"
fi

NVM_HOME="$HOME/.nvm"
NVM_SH="$NVM_HOME/nvm.sh"

. "$NVM_SH"
nvm ls | @{REMOVE_COLOR_CODES}

if [ $? -ne 0 ] || [ ! -f "$NVM_SH" ]; then
    rm -rf "$NVM_HOME"
    git clone    @{nvmRepo} "$NVM_HOME"
    cd  "$NVM_HOME"
    git checkout @{nvmCommit}
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

nvm ls @{nodeVersion}
if [ $? -ne 0 ]; then
    nvm install @{nodeVersion}
fi
nvm use     @{nodeVersion}

echo "npm  : [`which npm`][`npm --version`]"
echo "node : [`which node`][`node --version`]"

mkdir -p "$HOME/.npm"

set +e
echo $sudoCommand chown -R $user:$group "$HOME/.npm"
$sudoCommand chown -R $user:$group "$HOME/.npm"
set -e

echo npm install
npm install

if [ "@{ensureForever}" == "true" ] && [ ! -f "@{forever}" ];
then
    echo "npm install forever"
    npm install forever
fi
