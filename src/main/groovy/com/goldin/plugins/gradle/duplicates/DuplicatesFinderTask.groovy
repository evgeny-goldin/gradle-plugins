package com.goldin.plugins.gradle.duplicates

import com.goldin.plugins.gradle.util.BaseTask
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency

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
    private final Map<File, List<String>> classesCache = [:]


    @Override
    void taskAction ()
    {
        Map<String, Map<String, List<String>>> violations =
        ( Map ) project.configurations.inject( [:] ) {
            Map m, Configuration c ->

            m[ c.name ] = (( ! configurations ) || ( configurations.contains( c.name ))) ?
                            getViolations( c ) :
                            [:]
            m
        }.
        findAll {
            String configName, Map configViolations ->
            configViolations
        }

        if ( violations ) { reportViolations( violations ) }
    }


    /**
     * Gets all duplicate violations in Configuration specified.
     *
     * @param config configuration to check for duplicates
     * @return configuration violations as mapping:
     *         key   - violating dependencies, Stringified list
     *         value - duplicate classes found in those dependencies
     */
    @Requires({ config })
    @Ensures({ result != null })
    Map<String, List<String>> getViolations( Configuration config )
    {
        Set<File> configFiles = config.resolve().findAll { it.isFile() } // To filter out directories

        if ( ! configFiles ) { return [:] }

        Map<File, String> f2d = filesToDependencies( config )
        assert ( configFiles.size() == f2d.size()) && ( configFiles.each { f2d[ it ] } )

       /**
        * 1) Mapping of class names to files they're found in:
        *    Map<String, List<File>>
        */
        ( Map ) configFiles.inject( [:].withDefault{ [] } ){
            Map m, File f ->
            classNames( f ).each{ String className -> m[ className ] << f }
            m
        }.

       /**
        * 2) Classes with more than one file they're found in
        *    Map<String, List<File>>
        */
        findAll { String className, List<File> f -> f.size() > 1 }.

        /**
         * 3) Mapping of violating dependencies to duplicate classes
         *    Map<String, List<String>>
         */
        inject( [:].withDefault{ [] } ){
            Map m, Map.Entry<String, List<File>> entry ->

            // List<File> => List<String> (via f2d) => String
            String dependencies = entry.value.collect{ f2d[ it ] }.toString()
            // Adding duplicate class name to violation dependencies list of classes
            m[ dependencies ]  << entry.key
            m
        }
    }


   /**
    * Creates File => Dependency (as String) mapping for Configuration specified.
    *
    * @param config Configuration to create the mapping for
    * @return Mapping:
    *         key   - File
    *         value - Dependency the file is originated from (as "group:name:version")
    */
    @Requires({ config })
    @Ensures({ result != null })
    Map<File, String> filesToDependencies ( Configuration config )
    {
        ResolvedConfiguration rc = config.resolvedConfiguration

        ( Map ) rc.resolvedArtifacts*.resolvedDependency.
        unique {
            ResolvedDependency rd1, ResolvedDependency rd2 ->
            (( rd1.moduleGroup   != rd2.moduleGroup   ) ||
             ( rd1.moduleName    != rd2.moduleName    ) ||
             ( rd1.moduleVersion != rd2.moduleVersion )) ? 1 : 0
        }.
        inject( [:] ) {
            Map m, ResolvedDependency rd ->
            File   dependencyFile = rd.allModuleArtifacts.iterator().next().file
            assert dependencyFile.isFile()

            m[ dependencyFile ] = "$rd.moduleGroup:$rd.moduleName:$rd.moduleVersion"
            m
        }
    }


    /**
     * Reads Zip archive and returns list of class names stored in it.
     *
     * @param file Zip archive to read
     * @return list of class names stored in it
     */
    @Requires({ file.isFile() })
    @Ensures({ result != null })
    List<String> classNames ( File file )
    {
        if ( classesCache.containsKey( file ))
        {
            return classesCache[ file ]
        }

        ZipFile zip = new ZipFile( file )

        try
        {
            classesCache[ file ] =
                zip.entries().findAll{ ZipEntry entry -> entry.name.endsWith( '.class' ) }.
                              collect{ ZipEntry entry -> entry.name.replace( '/', '.' ).
                                                                    replaceAll( /\.class$/, '' ) }
        }
        finally { zip.close() }
    }


    /**
     * Reports violations found by throwing an exception or logging an error message.
     *
     * @param violations violations found, as mapping:
     *        key   - configuration name
     *        value - configuration violations, as mapping:
     *                key   - violating dependencies, Stringified list
     *                value - duplicate classes found in violating dependencies
     */
    @Requires({ violations })
    void reportViolations( Map<String, Map<String, List<String>>> violations )
    {
        def message = violations.collect {
            String configName, Map<String, List<String>> configViolations ->

            assert configViolations

            [ "\nConfiguration [$configName] - duplicates found in:" ] +
            configViolations.collect{ String dependencies, List<String> classes ->
                                      [ "-=-= $dependencies =-=-" ] +
                                      ( verbose ? classes.sort().collect { " --- [$it]" } : [] )}
        }.
        flatten().
        findAll{ it }.
        join( '\n' )

        if ( fail ) { throw new RuntimeException( message )}
        else        { project.logger.error( message )}
    }
}
