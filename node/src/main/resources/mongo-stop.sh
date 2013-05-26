#!@{shell}

set -e
echo "Stopping Mongo [127.0.0.1:@{mongoPort}]"
mongod --version
mongo  --version
set +e

if [[ @{mongoRunning} ]];
then
    echo mongo  --eval "db.getSiblingDB('admin').shutdownServer()" --port @{mongoPort}
    mongo  --eval "db.getSiblingDB('admin').shutdownServer()" --port @{mongoPort}

    echo Waiting for @{sleep} seconds before continuing
    sleep @{sleep}

    if [[ @{mongoRunning} ]];
    then
        echo "Mongo [127.0.0.1:@{mongoPort}] has failed to stop"
        exit 1
    else
        echo "Mongo [127.0.0.1:@{mongoPort}] has stopped"
    fi
else
    echo "Mongo [127.0.0.1:@{mongoPort}] isn't running"
fi
