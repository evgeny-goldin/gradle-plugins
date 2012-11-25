package com.github.goldin.plugins.gradle.duplicates

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.logging.LogLevel

import java.util.zip.ZipEntry
import java.util.zip.ZipFile


/**
 * Searches for duplicates in configurations provided
 */
class DuplicatesTask extends BaseTask<DuplicatesExtension>
{
    /**
     * Cache of file on the disk to classes it contains
     */
    private final Map<File, List<String>> classesCache = [:]

    @Override
    void verifyUpdateExtension ( String description ) {}

    @Override
    void taskAction ()
    {
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
        final Collection<ResolvedArtifact> configurationArtifacts =
            configuration.resolvedConfiguration.resolvedArtifacts.findAll { it.file.file }

        if ( ! configurationArtifacts ) { return [:] }

        /**
         * Mapping of files to their original dependencies:
         * - Keys are configuration's files.
         * - Values are file dependencies, in a "<group>:<name>:<version>" form.
         */

        Map<File, String> f2d = configurationArtifacts.inject([:]) {
            Map m, ResolvedArtifact artifact ->
            artifact.moduleVersion.id.with { m[ artifact.file ] = "$group:$name:$version" }
            m
        }

       /**
        * Mapping of class names to files they're found in:
        * - Keys are class names.
        * - Values are list of files they're found in.
        */

        ( Map ) configurationArtifacts*.file.inject( [:].withDefault{ [] } ){
            Map m, File f ->
            classNames( f ).each{ String className -> m[ className ] << f }
            m
        }.

       /**
        * Classes with more than one file they're found in:
        * - Keys are class names.
        * - Values are list of files they're found in, larger than one - meaning there's a violation.
        */

        findAll { String className, List<File> files -> ( files.size() > 1 )}.

        /**
         * Mapping of violating dependencies to duplicate classes:
         * - Keys are list of dependencies, stringified.
         * - Values are list of duplicate class names.
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
     * Reads Zip archive and returns list of class names stored in it.
     *
     * @param file Zip archive to read
     * @return list of class names stored in it
     */
    @Requires({ file.file })
    @Ensures({ result != null })
    List<String> classNames ( File file )
    {
        if ( classesCache.containsKey( file ))
        {
            return classesCache[ file ]
        }

        ZipFile zip = null

        try
        {
            zip = new ZipFile( file )
            classesCache[ file ] =
                zip.entries().findAll{ ZipEntry entry -> entry.name.endsWith( '.class' ) }.
                              collect{ ZipEntry entry -> entry.name.replace( '/', '.' ).
                                                                    replaceAll( /\.class$/, '' ) }
        }
        catch ( e )
        {
            log( LogLevel.ERROR, e ){ "Unable to read Zip entries of [$file.canonicalPath]" }
            []
        }
        finally
        {
            if ( zip ) { zip.close() }
         }
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
    @Requires({ violations })
    void reportViolations( Map<String, Map<String, List<String>>> violations )
    {
        final message = violations.collect {
            String configurationName, Map<String, List<String>> configurationViolations ->

            assert configurationViolations

            [ "\nConfiguration [$configurationName] - duplicates found in:" ] +
            configurationViolations.collect {
                String dependencies, List<String> classes ->
                [ "-=-= $dependencies =-=-" ] + ( ext.verbose ? classes.sort().collect { " --- [$it]" } : [] )}
        }.
        flatten().
        grep().
        join( '\n' )

        if ( ext.fail ) { throw new RuntimeException( message )}
        else            { log( LogLevel.ERROR ){ message }}
    }
}
