#!@{shell}

set -e
set -o pipefail

echo "Starting MongoDB [127.0.0.1:@{mongoPort}]"
mongod --version
mongo  --version

mkdir -p @{mongoDBPath}

if [[ @{mongoRunning} ]];
then
    echo "Mongo [127.0.0.1:@{mongoPort}] is already running"
else
    echo mongod --fork --port @{mongoPort} --dbpath @{mongoDBPath} --logpath @{mongoLogpath} @{mongoCommandLine}
    mongod --fork --port @{mongoPort} --dbpath @{mongoDBPath} --logpath @{mongoLogpath} @{mongoCommandLine}

    echo Waiting for @{sleep} seconds before continuing
    sleep @{sleep}

    if [[ @{mongoRunning} ]];
    then
        echo "Mongo [127.0.0.1:@{mongoPort}] has started"
    else
        echo "Mongo [127.0.0.1:@{mongoPort}] has failed to start"
        exit 1
    fi
fi

mongo --eval "print('\n=========\nHost Info\n=========\n'); printjson(db.hostInfo()); print('\n========\nDB Stats\n========\n'); printjson(db.stats())" --port @{mongoPort}
