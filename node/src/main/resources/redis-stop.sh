#!/bin/bash

set -e
set -o pipefail

echo "Stopping Redis [127.0.0.1:${redisPort}], command line [${redisCommandLine}]"
echo "redis-server --version" : [`redis-server --version`]
echo "redis-cli    --version" : [`redis-cli    --version`]

if [ ${redisRunning} ];
then
    redis-cli -p ${redisPort} shutdown
    sleep ${sleep}

    if [ ${redisRunning} ];
    then
        echo "Redis [127.0.0.1:${redisPort}] has failed to stop"
        exit 1
    else
        echo "Redis [127.0.0.1:${redisPort}] has stopped"
    fi
else
    echo "Redis [127.0.0.1:${redisPort}] isn't running"
fi
