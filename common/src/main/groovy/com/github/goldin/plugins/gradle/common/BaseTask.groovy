package com.github.goldin.plugins.gradle.common

import org.apache.tools.ant.DirectoryScanner
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory


/**
 * Base helper task class to be extended by other tasks
 */
abstract class BaseTask extends DefaultTask
{
    File   rootDir
    String version
    long   startTime


    /**
     * Should be implemented by task.
     * Called after all fields are initialized.
     */
    abstract void taskAction()


    @TaskAction
    @Requires({ project && project.rootDir && project.version })
    final void doTask()
    {
        this.rootDir   = project.rootDir
        this.version   = project.version
        this.startTime = System.currentTimeMillis()
        taskAction()
    }


    final String s( Collection c ){ s( c.size()) }
    final String s( Number     j ){ j == 1 ? '' : 's' }


    /**
     * Executes the command specified.
     *
     * @param command   command to execute
     * @param arguments command arguments, will be tokenized
     * @param directory process working directory
     * @return process standard and error output
     */
    final String exec( String command, String arguments, File directory = null )
    {
        exec( command, arguments.tokenize(), directory )
    }


    /**
     * Executes the command specified.
     *
     * @param command   command to execute
     * @param arguments command arguments
     * @param directory process working directory
     * @return process standard and error output
     */
    final String exec( String command, List<String> arguments, File directory = null )
    {
        assert command && ( arguments != null )
        if ( logger.infoEnabled )
        {
            logger.info( "Running [$command] with arguments $arguments${ directory ? ' in [' + directory.canonicalPath + ']' : '' }" )
        }

        final outputStream = new ByteArrayOutputStream()

        try
        {
            project.exec {
                ExecSpec spec ->
                spec.with {
                    executable( command )
                    args( arguments )
                    standardOutput = outputStream
                    errorOutput    = outputStream
                    if ( directory ) { workingDir = directory }
                }
            }
        }
        catch ( Throwable error )
        {
            throw new GradleException( "Failed to execute [$command] with arguments $arguments, output is [${ outputStream.toString().trim()}]",
                                       error )
        }

        if ( logger.debugEnabled )
        {
            final output = outputStream.toString().trim()
            if ( output ) { logger.debug( '>> ' + output.readLines().join( '\n>> ' )) }
        }
        outputStream.toString()
    }


    /**
     * Retrieves extension of the type specified.
     *
     * @param extensionName name of extension
     * @param extensionType type of extension
     * @return extension of the type specified
     */
    @Requires({ extensionName && extensionType })
    @Ensures ({ extensionType.isInstance( result ) })
    final public <T> T extension( String extensionName, Class<T> extensionType )
    {
        final  extension = project[ extensionName ]
        assert extensionType.isInstance( extension ), \
               "Project object (extension?) [$extensionName] is of type [${ extension.getClass().name }], " +
               "not assignment-compatible with type [${ extensionType.name }]"

        (( T ) extension )
    }


    /**
     * Retrieves files (and directories, if required) given base directory and inclusion/exclusion patterns.
     * Symbolic links are not followed.
     *
     * @param baseDirectory      files base directory
     * @param includePatterns    patterns to use for including files, all files are included if null
     * @param excludePatterns    patterns to use for excluding files, no files are excluded if null
     * @param isCaseSensitive    whether or not include and exclude patterns are matched in a case sensitive way
     * @param includeDirectories whether directories included should be returned as well
     * @param failIfNotFound     whether execution should fail if no files were found
     *
     * @return files under base directory specified passing an inclusion/exclusion patterns
     */
    @Requires({ baseDirectory.directory })
    final List<File> files ( File         baseDirectory,
                             List<String> includePatterns    = null,
                             List<String> excludePatterns    = null,
                             boolean      isCaseSensitive    = true,
                             boolean      includeDirectories = false,
                             boolean      failIfNotFound     = true )
    {
        def scanner = new DirectoryScanner()

        scanner.with {
            basedir           = baseDirectory
            includes          = includePatterns as String[]
            excludes          = excludePatterns as String[]
            caseSensitive     = isCaseSensitive
            errorOnMissingDir = true
            followSymlinks    = false
            scan()
        }

        def files = []
        scanner.includedFiles.each { String filePath -> files << new File( baseDirectory, filePath ) }

        if ( includeDirectories )
        {
            scanner.includedDirectories.findAll { it }.each { String dirPath -> files << new File( baseDirectory, dirPath ) }
        }

        assert ( files || ( ! failIfNotFound )), \
               "No files are included by parent dir [$baseDirectory] and include/exclude patterns ${ includePatterns ?: [] }/${ excludePatterns ?: [] }"

        files
    }


    /**
     * Deletes files specified.
     *
     * @param files files to delete, may be {@code null} or non-existing.
     */
    final File delete ( File ... files )
    {
        for ( file in files.grep())
        {
            if      ( file.file      ){ assert file.delete(),    "Failed to delete file [$file.canonicalPath]"      }
            else if ( file.directory ){ assert file.deleteDir(), "Failed to delete directory [$file.canonicalPath]" }
            assert  ( ! file.exists())
        }

        (( files.size() > 0 ) ? files[ 0 ] : null )
    }


    /**
     * Creates an archive specified.
     *
     * @param archive     archive to create
     * @param zipClosure  closure to run in {@code ant.zip{ .. }} context
     * @return archive created
     */
    @Requires({ archive && zipClosure })
    @Ensures ({ result.file })
    final File archive ( File archive, Closure zipClosure )
    {
        delete( archive )
        ant.zip( destfile: archive, duplicate: 'fail', whenempty: 'fail', level: 9 ){ zipClosure() }
        assert archive.file, "Failed to create [$archive.canonicalPath] using 'ant.zip( .. ){ .. }'"
        archive
    }


    /**
     * Adds files specified to the archive through {@code ant.zipfileset( file: file, prefix: prefix )}.
     *
     * @param archive  archive to add files specified
     * @param files    files to add to the archive
     * @param prefix   files prefix in the archive
     * @param includes patterns of files to include, all files are included if null or empty
     * @param excludes patterns of files to exclude, no files are excluded if null or empty
     */
    final void addFilesToArchive ( File             archive,
                                   Collection<File> files,
                                   String           prefix,
                                   List<String>     includes = null,
                                   List<String>     excludes = null )
    {
        files.each { addFileToArchive( archive, it, prefix, includes, excludes )}
    }


    /**
     * Adds file specified to the archive through {@code ant.zipfileset( file: file, prefix: prefix )}.
     *
     * @param archive  archive to add files specified
     * @param file     file to add to the archive
     * @param prefix   files prefix in the archive
     * @param includes patterns of files to include, all files are included if null or empty
     * @param excludes patterns of files to exclude, no files are excluded if null or empty
     */
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    final void addFileToArchive ( File         archive,
                                  File         file,
                                  String       prefix,
                                  List<String> includes = null,
                                  List<String> excludes = null )
    {
        assert archive && file && ( prefix != null )

        prefix = prefix.startsWith( '/' ) ? prefix.substring( 1 )                      : prefix
        prefix = prefix.endsWith  ( '/' ) ? prefix.substring( 0, prefix.length() - 1 ) : prefix

        assert ( file.file || file.directory ), \
               "[${ file.canonicalPath }] - not found when creating [${ archive.canonicalPath }]"

        final arguments = [ ( file.file ? 'file' : 'dir' ) : file, prefix: prefix ]
        if ( includes ) { arguments[ 'includes' ] = includes.join( ',' )}
        if ( excludes ) { arguments[ 'excludes' ] = excludes.join( ',' )}

        ant.zipfileset( arguments )
    }


    /**
     * Verifies resources specified can be found in files provided.
     *
     * @param files     files to check
     * @param resources resources to locate in the files provided
     */
    final void checkResources( Collection<File> files, String ... resources )
    {
        assert files && resources
        final cl = new URLClassLoader( files*.toURI()*.toURL() as URL[] )
        resources.each { assert cl.getResource( it ), "No '$it' resource found in $files" }
    }


    /**
     * Validates XML specified with Schema provided.
     *
     * @param xml    XML to validate
     * @param schema schema to validate with
     * @return       same XML instance
     * @throws       GradleException if validation fails
     */
    final String validateXml( String xml, String schema )
    {
        assert xml && schema

        try
        {
            SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI ).
            newSchema( new StreamSource( new StringReader( schema ))).
            newValidator().
            validate( new StreamSource( new StringReader( xml )))
        }
        catch ( e )
        {
            throw new GradleException( "Failed to validate XML\n[$xml]\nusing schema\n[$schema]", e )
        }

        xml
    }
}
