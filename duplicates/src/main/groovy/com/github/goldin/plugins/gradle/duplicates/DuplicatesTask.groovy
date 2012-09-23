package com.github.goldin.plugins.gradle.duplicates

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency

import java.util.zip.ZipEntry
import java.util.zip.ZipFile


/**
 * Searches for duplicates in configurations provided
 */
class DuplicatesTask extends BaseTask
{
    /**
     * Retrieves current plugin extension object.
     * @return current plugin extension object
     */
    DuplicatesExtension ext() { extension( 'duplicates', DuplicatesExtension ) }


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

            m[ c.name ] = (( ! ext().configurations ) || ( ext().configurations.contains( c.name ))) ?
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
    Map<String, List<String>> getViolations( Configuration config )
    {
        assert config
        Set<File> configFiles = config.resolve().findAll { it.file } // To filter out directories

        if ( ! configFiles ) { return [:] }

        Map<File, String> f2d = filesToDependencies( config )
        assert ( configFiles.size() == f2d.size()) && ( configFiles.every { f2d[ it ] } )

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
    Map<File, String> filesToDependencies ( Configuration config )
    {
        assert config
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
            assert dependencyFile.file

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
    List<String> classNames ( File file )
    {
        assert file.file

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
    @SuppressWarnings( 'UseCollectMany' )
    void reportViolations( Map<String, Map<String, List<String>>> violations )
    {
        assert violations

        def message = violations.collect {
            String configName, Map<String, List<String>> configViolations ->

            assert configViolations

            [ "\nConfiguration [$configName] - duplicates found in:" ] +
            configViolations.collect{ String dependencies, List<String> classes ->
                                      [ "-=-= $dependencies =-=-" ] +
                                      ( ext().verbose ? classes.sort().collect { " --- [$it]" } : [] )}
        }.
        flatten().
        findAll{ it }.
        join( '\n' )

        if ( ext().fail ) { throw new RuntimeException( message )}
        else            { project.logger.error( message )}
    }
}
