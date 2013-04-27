package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.file.CopySpec


/**
 * {@link com.github.goldin.plugins.gradle.node.tasks.SetupTask} helper.
 */
class SetupCacheHelper
{
    private final BaseTask      task
    private final NodeExtension ext


    @Requires({ task && ext })
    @Ensures ({ this.task && this.ext })
    SetupCacheHelper ( BaseTask task, NodeExtension ext )
    {
        this.task = task
        this.ext  = ext
    }


    void restoreNodeModulesFromCache ()
    {
        final nodeModules = new File ( task.projectDir, 'node_modules' )
        if ( nodeModules.directory ) { return }

        final npmCacheArchive = localArchive()
        if ( ! npmCacheArchive?.file ) { return }

        task.logger.info( "Unpacking 'npm install' cache [$npmCacheArchive.canonicalPath] to [$task.projectDir.canonicalPath]" )
        task.exec( 'tar', [ '-xzf', npmCacheArchive.canonicalPath, '-C', task.projectDir.canonicalPath ], task.projectDir )
    }


    @SuppressWarnings([ 'GroovyMultipleReturnPointsPerMethod' ])
    void createNodeModulesCache ()
    {
        final nodeModules = new File ( task.projectDir, 'node_modules' )
        if ( ! ( nodeModules.directory && ext.npmCleanInstall )) { return }

        final npmCacheArchive = localArchive()
        if (( npmCacheArchive == null ) || npmCacheArchive.file ) { return }

        task.logger.info( "Packing 'npm install' result [$nodeModules.canonicalPath] to [$npmCacheArchive.canonicalPath]" )
        final tempFile = task.project.file( npmCacheArchive.name )

        task.project.copy { CopySpec cs -> cs.from( PACKAGE_JSON ).into( nodeModules ) }
        updatePackageJson( new File( nodeModules, PACKAGE_JSON ))

        task.exec( 'tar', [ '-czf', tempFile.canonicalPath, nodeModules.name ], task.projectDir )
        assert tempFile.renameTo( npmCacheArchive ), "Failed to rename [$tempFile.canonicalPath] to [$npmCacheArchive.canonicalPath]"
    }


    /**
     * Retrieves "npm install" local cache archive, if available.
     * @return "npm install" local cache archive or
     *         {@code null} if unable to calculate "package.json" dependencies checksum
     */
    private File localArchive ()
    {
        final checksum = packageJsonChecksum()
        if ( ! checksum ) { return null }

        final npmCacheArchive = new File( System.getProperty( 'user.home' ), ".npm/${ checksum }.tar.gz" )
        npmCacheArchive.parentFile.with { File f -> assert ( f.directory || f.mkdirs()), "Failed to mkdir [$f.canonicalPath]" }

        if ( ext.npmRemoteCache )
        {
            npmCacheArchive.file ? uploadLocalArchive( npmCacheArchive ) : downloadRemoteArchive( npmCacheArchive )
        }

        npmCacheArchive
    }


    /**
     * Calculates checksum of "package.json" dependencies.
     * @return checksum of "package.json" dependencies or empty {@code String}
     */
    @Ensures({ result != null })
    private String packageJsonChecksum ()
    {
        final packageJson = task.project.file( PACKAGE_JSON )
        if ( ! packageJson.file ) { return '' }

        final Map<String,?> packageMap      = task.jsonToMap( packageJson.getText( 'UTF-8' ), packageJson )
        final Map<String,?> dependenciesMap = ( packageMap.dependencies ?: [:] ) + ( packageMap.devDependencies ?: [:] )
        if ( ! dependenciesMap ){ return '' }

        final dependenciesString = dependenciesMap.keySet().sort().
                                   collect { "${ it.toLowerCase()  }:${ dependenciesMap[ it ].toString().toLowerCase() }" }.
                                   join( '\n' )

        task.checksum( dependenciesString )
    }


    @Requires({ npmCacheArchive && ( ! npmCacheArchive.file ) && ext.npmRemoteCache })
    private void downloadRemoteArchive( File npmCacheArchive )
    {
        final map        = readRemoteRepoUrl( npmCacheArchive )
        final archiveUrl = map.archiveUrl

        task.logger.info( "Downloading [$archiveUrl] to [$npmCacheArchive.canonicalPath] .." )

        final response   = task.httpRequest( archiveUrl, 'GET', [:], 0, 0, null, false, true, true, map.user, map.password )

        if ( ! (( response.statusCode == 200 ) && response.data )){ return }

        npmCacheArchive.withOutputStream { OutputStream os -> os.write( response.data )}
        task.logger.info( "Downloaded [$archiveUrl] to [$npmCacheArchive.canonicalPath]" )
    }


    @Requires({ npmCacheArchive && npmCacheArchive.file && ext.npmRemoteCache })
    private void uploadLocalArchive ( File npmCacheArchive )
    {
        final map        = readRemoteRepoUrl( npmCacheArchive )
        final archiveUrl = map.archiveUrl

        if ( task.httpRequest( archiveUrl, 'HEAD', [:], 0, 0, null, false, false ).statusCode != 404 ) { return }

        task.logger.info( "Uploading [$npmCacheArchive.canonicalPath] to [$archiveUrl] .." )

        task.httpRequest( archiveUrl, 'PUT', [:], 0, 0, null, true, true, true,
                          map.user, map.password, npmCacheArchive.bytes )

        assert ( task.httpRequest( archiveUrl, 'HEAD' ).statusCode == 200 ), \
               "Failed to upload [$npmCacheArchive.canonicalPath] to [$archiveUrl]"

        task.logger.info( "Uploaded [$npmCacheArchive.canonicalPath] to [$archiveUrl]" )
    }


    @Requires({ npmCacheArchive && ext.npmRemoteCache })
    @Ensures ({ result })
    private Map<String, String> readRemoteRepoUrl( File npmCacheArchive )
    {
        // ext.npmRemoteCache = "deployer:password@http://evgenyg.artifactoryonline.com/evgenyg/npm/"

        final user       = ext.npmRemoteCache.find( /^(.+?):/         ){ it[1] } ?: ''
        final password   = ext.npmRemoteCache.find( /^.+?:(.+?)@/     ){ it[1] } ?: ''
        final repoUrl    = ext.npmRemoteCache.find( /^.+?:.+?@(.+?)$/ ){ it[1] } ?: ext.npmRemoteCache
        final archiveUrl = "$repoUrl${ repoUrl.endsWith( '/' ) ? '' : '/' }$npmCacheArchive.name"

        [ user: user, password: password, archiveUrl: archiveUrl ]
    }




    /**
     * Updates "package.json" specified with cache-related data.
     *
     * @param packageJson "package.json" file to update
     */
    @Requires({ packageJson.file })
    private void updatePackageJson( File packageJson )
    {
        final packageJsonMap     = task.jsonToMap( packageJson.getText( 'UTF-8' ), packageJson )

        packageJsonMap.cacheData = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
        packageJsonMap.timestamp = task.dateFormatter.format( new Date())
        packageJsonMap.host      = "${ task.hostname() }/${ InetAddress.localHost.hostAddress }".toString()
        packageJsonMap.project   = task.project.toString()
        packageJsonMap.directory = task.projectDir.canonicalPath
        packageJsonMap.origin    = task.gitExec( 'remote -v', task.projectDir ).readLines().find { it.startsWith( 'origin' )}.split()[ 1 ]
        packageJsonMap.sha       = task.gitExec( 'log -1 --format=format:%H', task.projectDir )

        packageJson.setText( task.objectToJson( packageJsonMap ), 'UTF-8' )
    }
}
