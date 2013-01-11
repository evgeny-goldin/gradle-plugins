#!/bin/bash

set -e
set -o pipefail

echo "Starting Redis [127.0.0.1:${redisPort}]"
echo "redis-server --version" : [`redis-server --version`]
echo "redis-cli    --version" : [`redis-cli    --version`]

if [ ${redisRunning} ];
then
    echo "Redis [127.0.0.1:${redisPort}] is already running"
else
    export BUILD_ID=JenkinsLetMeSpawn
    redis-server --port ${redisPort} &
    sleep 3

    if [ ${redisRunning} ];
    then
        echo "Redis [127.0.0.1:${redisPort}] has started"
    else
        echo "Redis [127.0.0.1:${redisPort}] has failed to start"
        exit 1
    fi
fi

redis-cli -p ${redisPort} info
