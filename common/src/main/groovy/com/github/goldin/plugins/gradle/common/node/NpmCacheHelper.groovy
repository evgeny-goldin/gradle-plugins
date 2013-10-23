package com.github.goldin.plugins.gradle.common.node

import static com.github.goldin.plugins.gradle.common.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import org.gradle.api.file.CopySpec


/**
 * Helper for the local "npm" cache.
 */
class NpmCacheHelper extends BaseHelper<NodeBaseExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    NpmCacheHelper ( Project project, BaseTask task, NodeBaseExtension ext ){ super( project, task, ext )}


    @SuppressWarnings([ 'GroovyMultipleReturnPointsPerMethod' ])
    void restoreNodeModulesFromCache()
    {
        if ( ! ext.npmLocalCache ){ return }

        final nodeModules = new File ( projectDir, 'node_modules' )
        if ( nodeModules.directory ) { return }

        final npmCacheArchive = localArchive( true )
        if ( ! npmCacheArchive?.file ) { return }

        log { "Unpacking 'npm install' cache [$npmCacheArchive.canonicalPath] to [$projectDir.canonicalPath]" }
        exec( 'tar', [ '-xzf', npmCacheArchive.canonicalPath, '-C', projectDir.canonicalPath ] )
    }


    @SuppressWarnings([ 'GroovyMultipleReturnPointsPerMethod' ])
    void createNodeModulesCache()
    {
        if ( ! ext.npmLocalCache ){ return }

        final nodeModules = new File ( projectDir, 'node_modules' )
        if ( ! ( nodeModules.directory && ext.npmCleanInstall )) { return }

        final npmCacheArchive = localArchive( false )
        if (( npmCacheArchive == null ) || npmCacheArchive.file ) { return }

        log{ "Packing 'npm install' result [$nodeModules.canonicalPath] to [$npmCacheArchive.canonicalPath]" }

        project.copy { CopySpec cs -> cs.from( PACKAGE_JSON ); cs.into( nodeModules ) }
        updatePackageJson( new File( nodeModules, PACKAGE_JSON ))
        final tempFile = project.file( npmCacheArchive.name )

        exec( 'tar', [ '-czf', tempFile.canonicalPath, nodeModules.name ], projectDir ) // Use relative path, not absolute!
        assert tempFile.renameTo( npmCacheArchive ) && npmCacheArchive.file, \
               "Failed to rename [$tempFile.canonicalPath] to [$npmCacheArchive.canonicalPath]"

        if ( ext.npmRemoteCache ){ uploadLocalArchive( npmCacheArchive ) }
    }


    /**
     * Retrieves "npm install" local cache archive, if available.
     * @return "npm install" local cache archive or
     *         {@code null} if unable to calculate "package.json" dependencies checksum
     */
    private File localArchive ( boolean attemptDownloading )
    {
        final checksum = packageJsonChecksum()
        if ( ! checksum ) { return null }

        final npmCacheArchive = home( ".npm/node_modules_${ checksum }.tar.gz" )
        npmCacheArchive.parentFile.with { File f -> assert ( f.directory || f.mkdirs()), "Failed to mkdir [$f.canonicalPath]" }

        if ( ext.npmRemoteCache )
        {
            npmCacheArchive.file ? uploadLocalArchive( npmCacheArchive ) :
            attemptDownloading   ? downloadRemoteArchive( npmCacheArchive ) :
                                   null
        }

        npmCacheArchive
    }


    /**
     * Calculates checksum of "package.json" dependencies.
     * @return checksum of "package.json" dependencies or empty {@code String}
     */
    @Ensures({ result != null })
    private String packageJsonChecksum()
    {
        final packageJson = project.file( PACKAGE_JSON )
        if ( ! packageJson.file ) { return '' }

        final Map<String,?> packageMap      = jsonToMap( packageJson )
        final Map<String,?> dependenciesMap = ( packageMap.dependencies ?: [:] ) + ( packageMap.devDependencies ?: [:] )
        if ( ! dependenciesMap ){ return '' }

        final dependenciesString = dependenciesMap.keySet()*.toLowerCase().sort().
                                   collect { "$it:${ dependenciesMap[ it ].toString().toLowerCase() }" }.
                                   join( '\n' )

        checksum( dependenciesString )
    }


    @Requires({ npmCacheArchive && ( ! npmCacheArchive.file ) && ext.npmLocalCache && ext.npmRemoteCache })
    private void downloadRemoteArchive( File npmCacheArchive )
    {
        final map               = readRemoteRepoUrl( npmCacheArchive )
        final String archiveUrl = map.archiveUrl

        log{ "Downloading [$archiveUrl] to [$npmCacheArchive.canonicalPath] .." }

        final response = httpRequest( archiveUrl, 'GET', [:], 0, 0, false, true, map.user, map.password )
        if ( ! (( response.statusCode == 200 ) && response.data )){ return }

        final tempFile = project.file( npmCacheArchive.name )
        tempFile.withOutputStream { OutputStream os -> os.write( response.data )}
        assert tempFile.renameTo( npmCacheArchive ) && npmCacheArchive.file, \
               "Failed to rename [$tempFile.canonicalPath] to [$npmCacheArchive.canonicalPath]"

        log{ "Downloaded [$archiveUrl] to [$npmCacheArchive.canonicalPath]" }
    }


    @Requires({ npmCacheArchive && npmCacheArchive.file && ext.npmLocalCache && ext.npmRemoteCache })
    private void uploadLocalArchive ( File npmCacheArchive )
    {
        final map               = readRemoteRepoUrl( npmCacheArchive )
        final String archiveUrl = map.archiveUrl

        if ( httpRequest( archiveUrl, 'HEAD', [:], 0, 0, false, false, map.user, map.password ).
             statusCode != 404 ) { return }

        log{ "Uploading [$npmCacheArchive.canonicalPath] to [$archiveUrl] .." }

        httpRequest( archiveUrl, 'PUT', [:], 0, 0, true, true, map.user, map.password, null, npmCacheArchive.bytes )

        assert ( httpRequest( archiveUrl, 'HEAD', [:], 0, 0, false, true, map.user, map.password ).
                 statusCode == 200 ), "Failed to upload [$npmCacheArchive.canonicalPath] to [$archiveUrl]"

        log{ "Uploaded [$npmCacheArchive.canonicalPath] to [$archiveUrl]" }
    }


    @Requires({ npmCacheArchive && ext.npmRemoteCache })
    @Ensures ({ result })
    private Map<String, String> readRemoteRepoUrl( File npmCacheArchive )
    {
        // ext.npmRemoteCache = "user:password@http://evgenyg.artifactoryonline.com/evgenyg/npm/"

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
        final packageJsonMap     = jsonToMap( packageJson )

        packageJsonMap.cacheData = '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
        packageJsonMap.timestamp = format( System.currentTimeMillis())
        packageJsonMap.host      = "${ hostname() }".toString()
        packageJsonMap.project   = project.toString()
        packageJsonMap.directory = projectDir.canonicalPath
        packageJsonMap.origin    = gitExec( 'remote -v' ).readLines().find { it.startsWith( 'origin' )}.split()[ 1 ]
        packageJsonMap.sha       = gitExec( 'log -1 --format=format:%H' )

        objectToJson( packageJsonMap, packageJson )
    }
}
