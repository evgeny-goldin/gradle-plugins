#!/bin/bash

set -e
set -o pipefail

echo "Stopping Mongo [127.0.0.1:@{mongoPort}]"
echo "mongod --version" : [`mongod --version | head -1`]
echo "mongo  --version" : [`mongo --version`]

if [[ @{mongoRunning} ]];
then
    echo mongo  --eval "db.getSiblingDB('admin').shutdownServer()" --port @{mongoPort}
    mongo  --eval "db.getSiblingDB('admin').shutdownServer()" --port @{mongoPort}

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
