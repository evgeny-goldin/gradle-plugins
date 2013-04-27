package com.github.goldin.plugins.gradle.node

import static com.github.goldin.plugins.gradle.node.NodeConstants.*
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * {@link com.github.goldin.plugins.gradle.node.tasks.SetupTask} helper.
 */
class SetupCacheHelper
{
    private final BaseTask task


    @Requires({ task })
    SetupCacheHelper ( BaseTask task )
    {
        this.task = task
    }


    /**
     * Retrieves "npm install" local cache location, if available.
     * @return "npm install" local cache or {@code null} if unable to calculate "package.json" dependencies checksum
     */
    File localArchive ()
    {
        final checksum = packageJsonChecksum()
        if ( ! checksum ) { return null }
        new File( System.getProperty( 'user.home' ), ".npm/${ checksum }.tar.gz" )
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
}
