#!@{shell}

set -e
set -o pipefail

echo "Stopping Redis [127.0.0.1:@{redisPort}]"
redis-server --version
redis-cli    --version

if [ @{redisRunning} ];
then
    echo redis-cli -p @{redisPort} shutdown
    redis-cli -p @{redisPort} shutdown

    echo Waiting for @{sleep} seconds before continuing
    sleep @{sleep}

    if [ @{redisRunning} ];
    then
        echo "Redis [127.0.0.1:@{redisPort}] has failed to stop"
        exit 1
    else
        echo "Redis [127.0.0.1:@{redisPort}] has stopped"
    fi
else
    echo "Redis [127.0.0.1:@{redisPort}] isn't running"
fi
