package com.github.goldin.plugins.gradle.duplicates
import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact

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
    DuplicatesExtension ext() { extension( DuplicatesPlugin.EXTENSION_NAME, DuplicatesExtension ) }


    /**
     * Cache of file on the disk to classes it contains
     */
    private final Map<File, List<String>> classesCache = [:]


    @Override
    void taskAction ()
    {
        final ext = ext()

        Map<String, Map<String, List<String>>> violations =
        ( Map ) project.configurations.inject( [:] ) {
            Map m, Configuration c ->

            m[ c.name ] = (( ! ext.configurations ) || ( ext.configurations.contains( c.name ))) ?
                            getViolations( c ) :
                            [:]
            m
        }.
        findAll {
            String configurationName, Map violations ->
            violations
        }

        if ( violations ) { reportViolations( violations ) }
    }


    /**
     * Gets all duplicate violations in the Configuration specified.
     *
     * @param configuration configuration to check for duplicates
     * @return configuration violations as mapping:
     *         key   - violating dependencies, Stringified list
     *         value - duplicate classes found in those dependencies
     */
    @Requires({ configuration })
    @Ensures({ result != null })
    Map<String, List<String>> getViolations( Configuration configuration )
    {
        Collection<File> configurationFiles = configuration.resolve().findAll{ it.file }

        if ( ! configurationFiles ) { return [:] }

        /**
         * Mapping of files to their original dependencies: Map<File, String>
         * Values are of "<group>:<name>:<version>" form.
         */

        Map<File, String> f2d = filesToDependencies( configuration )
        assert ( f2d.size() == configurationFiles.size()), \
               "Files => Dependencies mapping size (${ f2d.size()}) doesn't match " +
               "configuration files amount (${ configurationFiles.size()}):\n\n" +
               "$f2d\n\n$configurationFiles"

        configurationFiles.each { assert f2d[ it ] }

       /**
        * Mapping of class names to files they're found in: Map<String, List<File>>
        */

        ( Map ) configurationFiles.inject( [:].withDefault{ [] } ){
            Map m, File f ->
            classNames( f ).each{ String className -> m[ className ] << f }
            m
        }.

       /**
        * Classes with more than one file they're found in: Map<String, List<File>>
        */

        findAll { String className, List<File> files -> ( files.size() > 1 )}.

        /**
         * Mapping of violating dependencies to duplicate classes: Map<String, List<String>>
         */

        inject( [:].withDefault{ [] } ){
            Map m, Map.Entry<String, List<File>> entry ->

            // List<File> => List<String> (via f2d) => String
            String violatingDependencies = entry.value.collect{ f2d[ it ] }.toString()
            // Adding duplicate class name to violation dependencies list of classes
            m[ violatingDependencies ] << entry.key
            m
        }
    }


   /**
    * Creates File => Dependency (as String) mapping for Configuration specified.
    *
    * @param configuration Configuration to create the mapping for
    * @return Mapping:
    *         key   - File
    *         value - Dependency the file is originated from (as "group:name:version")
    */
    @Requires({ configuration })
    @Ensures({ result != null })
    Map<File, String> filesToDependencies ( Configuration configuration )
    {
        configuration.resolvedConfiguration.resolvedArtifacts.inject([:]) {
            Map m, ResolvedArtifact artifact ->
            artifact.moduleVersion.id.with { m[ artifact.file ] = "$group:$name:$version" }
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

        final message = violations.collect {
            String configurationName, Map<String, List<String>> configurationViolations ->

            assert configurationViolations

            [ "\nConfiguration [$configurationName] - duplicates found in:" ] +
            configurationViolations.collect {
                String dependencies, List<String> classes ->
                [ "-=-= $dependencies =-=-" ] + ( ext().verbose ? classes.sort().collect { " --- [$it]" } : [] )}
        }.
        flatten().
        grep().
        join( '\n' )

        if ( ext().fail ) { throw new RuntimeException( message )}
        else              { project.logger.error( message )}
    }
}
