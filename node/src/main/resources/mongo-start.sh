#!@{shell}

set -e
echo "Starting MongoDB [127.0.0.1:@{mongoPort}]"
mongod --version
mongo  --version
set +e

if ! [ -d "@{mongoDBPath}" ]; then
  echo "mkdir -p @{Q}@{mongoDBPath}@{Q}"
  mkdir -p "@{mongoDBPath}"

  if ! [ -d "@{mongoDBPath}" ]; then
    echo "Failed to create @{Q}@{mongoDBPath}@{Q}"
    exit 1
  fi
fi

if [[ @{mongoRunning} ]];
then
    echo "Mongo [127.0.0.1:@{mongoPort}] is already running"
else
    echo "mongod --fork --port @{mongoPort} --dbpath @{Q}@{mongoDBPath}@{Q} --logpath @{Q}@{mongoLogpath}@{Q} @{mongoCommandLine}"
    mongod --fork --port @{mongoPort} --dbpath "@{mongoDBPath}" --logpath "@{mongoLogpath}" @{mongoCommandLine}

    echo Waiting for @{sleep} seconds before continuing
    sleep @{sleep}

    if [[ @{mongoRunning} ]];
    then
        echo "Mongo [127.0.0.1:@{mongoPort}] has started"
    else
        echo "Mongo [127.0.0.1:@{mongoPort}] has failed to start"
        echo "cat @{Q}@{mongoLogpath}@{Q}:"
        cat "@{mongoLogpath}"
        exit 1
    fi
fi

mongo --eval "print('\n=========\nHost Info\n=========\n'); printjson(db.hostInfo()); print('\n========\nDB Stats\n========\n'); printjson(db.stats())" --port @{mongoPort}
