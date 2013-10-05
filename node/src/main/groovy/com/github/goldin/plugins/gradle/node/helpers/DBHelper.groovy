package com.github.goldin.plugins.gradle.node.helpers

import static com.github.goldin.plugins.gradle.common.CommonConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import com.github.goldin.plugins.gradle.node.NodeExtension
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * Various helper methods for the Node plugin tasks.
 */
class DBHelper extends BaseHelper<NodeExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    DBHelper ( Project project, BaseTask task, NodeExtension ext ){ super( project, task, ext )}


    /**
     * Adds Redis before/after steps, if needed.
     */
    void addRedis()
    {
        final addRedis  = (( ext.redisPort > 0 ) || ext.redisPortConfigKey || ext.redisCommandLine )
        if (  addRedis )
        {
            final redisPort      = ( ext.redisPort > 0      ) ? ext.redisPort.toString() :
                                   ( ext.redisPortConfigKey ) ? '${ config.' + ext.redisPortConfigKey + ' }' :
                                                                '6379'
            ext.env.REDIS_PORT   = redisPort.startsWith( '$' ) ? '' : redisPort
            final redisRunning   = """ "`redis-cli -p $redisPort ping 2>&1`" = "PONG"  """.trim()
            final isStartRedis   = isRun( ext.redisStartInProduction )
            final isStopRedis    = isRun( ext.redisStopInProduction  )
            final getRedisScript = { String scriptName -> getResourceText( scriptName,
            [
                redisPort        : redisPort,
                redisRunning     : redisRunning,
                redisCommandLine : ext.redisCommandLine ?: '',
                sleep            : ext.redisWait as String,
                shell            : ext.shell,
                Q                : Q
            ])}

            addListeners( ext.redisListeners, isStartRedis ? getRedisScript( 'redis-start.sh' ) : '',
                                              isStopRedis  ? getRedisScript( 'redis-stop.sh'  ) : '' )
        }
    }


    /**
     * Adds MongoDB before/after steps, if needed.
     */
    void addMongo()
    {
        final addMongo = (( ext.mongoPort > 0 ) || ext.mongoPortConfigKey || ext.mongoCommandLine || ext.mongoLogpath || ext.mongoDBPath )
        if (  addMongo )
        {
            final mongoPort      = ( ext.mongoPort > 0      ) ? ext.mongoPort.toString() :
                                   ( ext.mongoPortConfigKey ) ? '${ config.' + ext.mongoPortConfigKey + ' }' :
                                                                '27017'
            ext.env.MONGO_PORT   = mongoPort.startsWith( '$' ) ? '' : mongoPort
            final mongoEval      = """ "`mongo --eval ${Q}db${Q} --port $mongoPort 2>&1 | tail -1`" """.trim()
            final mongoRunning   = """ ! $mongoEval =~ (command not found|connect failed|couldn\\'t connect to server) """.trim()
            final isStartMongo   = isRun( ext.mongoStartInProduction )
            final isStopMongo    = isRun( ext.mongoStopInProduction  )
            final getMongoScript = { String scriptName -> getResourceText( scriptName,
            [
                mongoPort        : mongoPort,
                mongoRunning     : mongoRunning,
                mongoDBPath      : fullPath( ext.mongoDBPath,  '/data/db'   ),
                mongoLogpath     : fullPath( ext.mongoLogpath, 'mongod.log' ),
                mongoCommandLine : ext.mongoCommandLine ?: '',
                sleep            : ext.mongoWait as String,
                shell            : ext.shell,
                Q                : Q
            ])}

            addListeners( ext.mongoListeners, isStartMongo ? getMongoScript( 'mongo-start.sh' ) : '',
                                              isStopMongo  ? getMongoScript( 'mongo-stop.sh'  ) : '' )
        }
    }


    private boolean isRun( boolean isRun ) {( isRun ) || ( ext.NODE_ENV != 'production' )}


    @SuppressWarnings([ 'GroovyMapGetCanBeKeyedAccess' ])
    @Requires({ listeners && ( beforeScript != null ) && ( afterScript != null ) })
    private void addListeners( List<String> listeners, String beforeScript, String afterScript )
    {
        for ( listener in listeners )
        {
            assert listener in [ 'before', 'after', 'beforeStart', 'afterStop', 'beforeTest', 'afterTest' ]
            final scriptLines = ( listener.startsWith( 'before' ) ? beforeScript : afterScript ).readLines()
            ext."$listener"   = ( scriptLines ?: [] ) + ( ext."$listener" ?: [] )
        }
    }
}
