Node.js Gradle plugin
----------------------

Sample configuration:

    node {
        NODE_ENV           = 'test'
        cleanWorkspace     = false
        cleanNodeModules   = false
        failOnTestFailures = false
        configs            = [ "config/${NODE_ENV}.json" : [ port               : 3001,
                                                             'db.core.user'     : 'MyUser',
                                                             'db.core.password' : 'MyPassword' ]]
    }

Assuming `config/test.json` is:

    {
     ...
      "port": 3000,
      "db": {
        "core": {
          "host"       : "...",
          "user"       : "...",
          "password"   : "...",
          "database"   : "..."
        }
      },
      ...
    }
