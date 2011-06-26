package com.goldin.plugins.gradle.duplicates

import com.goldin.plugins.gradle.util.BaseTask
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

/**
 * Searches for duplicates in configurations provided
 */
class DuplicatesFinderTask extends BaseTask
{
    List<String> configurations = null  // Default - all configurations are checked
    boolean      fail           = true  // Whether execution should fail when duplicates are found
    boolean      verbose        = false // Whether logging is verbose

    /**
     * Cache of file on the disk to classes it contains
     */
    private final Map<File, List<String>> CLASSES_CACHE = [:]


    @Override
    void taskAction ()
    {
        project.configurations.each {
            Configuration c ->

            if (( ! configurations ) || ( configurations.contains( c.name )))
            {
                Map<File, Dependency> f2d = ( Map ) c.dependencies.inject([:]) {
                    Map m, Dependency d ->
                    m[ c.files( d ).iterator().next() ] = d
                    m
                }

                checkConfiguration( c, f2d )
            }
        }
    }


    /**
     * Validates configuration specified contains no duplicate entries.
     *
     * @param c    configuration to check
     * @param f2d  mapping of files to their corresponding dependencies
     */
    private void checkConfiguration ( Configuration c, Map<File, Dependency> f2d )
    {
       /**
        * Mapping of class names to files they're found in
        */
        Map<String, List<File>> classes = ( Map ) c.files.inject( [:].withDefault{ [] } ){
            Map m, File f ->
            classNames( f ).each{ String className -> m[ className ] << f }
            m
        }

        /**
         * Mapping of violating dependencies (Stringified list) to duplicate class names
         */
        Map<String, List<String>> violations =

           /**
            * First, finding classes with more than one file they're found in
            */
            ( Map ) classes.findAll { String className, List<File> f -> f.size() > 1 }.

            /**
             * Second, violating classes are converted to mapping of violating dependencies:
             * List of dependencies => List of class names
             */
            inject( [:].withDefault{ [] } ){
                Map m, String className, List<File> classFiles ->

                // List<File> => List<Dependency> => List<String> => String
                String dependencies = classFiles.collect{ File       f -> f2d[ f ] }.
                                                 collect{ Dependency d -> "$d.group.$d.name.$d.version" }.
                                                 toString()
                m[ dependencies ] << className
                m
            }

        if ( violations ) { reportViolations( c.name, violations ) }
        else              { project.logger.info( "No duplicate libraries found in configuration [$c.name]" ) }
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
    private void reportViolations( String configurationName, Map<String, List<String>> violations )
    {
        def message =
            "\nConfiguration [$configurationName] - duplicates found in:\n" +
            violations.collect{
                String dependencies, List<String> classes ->
                [ "-=-= $dependencies =-=-" ] + ( verbose ? classes.sort().collect { " --- [$it]" } : [] )
            }.
            flatten(). // List of Lists => one flat List
            findAll{ it }.
            join( '\n' )

        if ( fail ) { throw new RuntimeException( message )}
        else        { project.logger.error( message )}
    }
}
