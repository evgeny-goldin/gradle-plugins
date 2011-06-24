package com.goldin.plugins.gradle.duplicates

import com.goldin.plugins.gradle.util.BaseTask
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

/**
 * Serches for duplicates in scopes provided
 */
class DuplicatesFinderTask extends BaseTask
{
    List<String> scopes  = null  // Default - all scopes are checked
    boolean      fail    = true  // Whether execution should fail when duplicates are found
    boolean      verbose = false // Whether logging is verbose

    /**
     * Cache of file on the disk to classes it contains
     */
    private final Map<File, List<String>> CLASSES_CACHE = [:]

    /**
     * Mapping of file to its dependency
     */
    private final Map<File, Dependency> F2D = [:]


    @Override
    void taskAction ()
    {
        project.configurations.each {
            Configuration c ->

            if (( ! scopes ) || ( scopes.contains( c.name )))
            {
                for ( Dependency d in c.dependencies )
                {
                    def files = c.files( d )
                    assert files.size() == 1
                    F2D[ files.iterator().next() ] = d
                }

                checkScope( c.name, c.iterator().collect{ it } )
            }
        }
    }


    /**
     * Validates scope specified contains no duplicate entries.
     *
     * @param scopeName name of the scope
     * @param files scope files resolved
     */
    private void checkScope( String scopeName, List<File> files )
    {
       /**
        * Mapping of class names to files they're found in
        */
        def classes = files.inject( [:].withDefault{ [] } ){
            Map m, File f ->
            classNames( f ).each{ String className -> m[ className ] << f }
            m
        }

        /**
         * Mapping of violating artifacts (Stringified list) to duplicate class names
         */
        Map<String, List<String>> violations =
            // First, finding classes with more than one file they're found in
            ( Map ) classes.findAll { String className, List<File> f -> f.size() > 1 }.
            // Second, violating classes are converted to mapping of violating dependencies: list of deps => list of class names
            inject( [:].withDefault{ [] } ){
                Map m, Map.Entry<String, List<File>> entry ->

                String className    = entry.key
                //                    List<File> => List<Dependency> => String
                String dependencies = entry.value.collect{ F2D[ it ] }.toString()
                m[ dependencies ]  << className
                m
            }

        if ( violations ) { reportViolations( scopeName, violations ) }
        else              { project.logger.info( "No duplicate libraries found" ) }
    }


    /**
     * Reads Zip archive and returns a list of class names stored in it.
     *
     * @param file Zip archive to read
     * @return list of class names stored in it
     */
    private List<String> classNames ( File file )
    {
        if ( CLASSES_CACHE.containsKey( file ))
        {
            return CLASSES_CACHE[ file ]
        }

        assert file.isFile()
        ZipFile zip = new ZipFile( file )

        try
        {
            CLASSES_CACHE[ file ] =
                zip.entries().findAll{ ZipEntry entry -> entry.name.endsWith( '.class' ) }.
                              collect{ ZipEntry entry -> entry.name.replace( '/', '.' ).
                                                               replaceAll( /\.class$/, '' )}
        }

        finally { zip.close() }
    }


    /**
     * Reports violations found by throwing an exception or logging an error message.
     *
     * @param violations violations found
     */
    private void reportViolations( String scopeName, Map<String, List<String>> violations )
    {
        def message =
            '\nScope [$scopeName] - duplicates found in:\n' +
            violations.collect{ String dependencies, List<String> classes ->
                                [ "-=-= $dependencies =-=-" ] + ( verbose ? classes.sort().collect { " --- [$it]" } : [] ) }.
                       flatten().
                       findAll{ it }.
                       join( '\n' )

        if ( fail ) { throw new RuntimeException( message )}
        else        { project.logger.error( message )}
    }
}
