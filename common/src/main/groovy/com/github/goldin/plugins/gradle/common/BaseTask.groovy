package com.github.goldin.plugins.gradle.common

import org.apache.tools.ant.DirectoryScanner
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import java.text.SimpleDateFormat
import java.util.regex.Pattern


/**
 * Base helper task class to be extended by other tasks
 */
abstract class BaseTask<T> extends DefaultTask
{
    final dateFormatter      = new SimpleDateFormat( 'dd MMM, EEEE, yyyy, HH:mm:ss (zzzzzz:\'GMT\'ZZZZZZ)', Locale.ENGLISH )
    final startTime          = System.currentTimeMillis()
    final startTimeFormatted = this.dateFormatter.format( new Date( this.startTime ))


    /**
     * Extension instance and its name are set by {@link BasePlugin#addTask}
     */
    T      ext
    String extensionName


    @TaskAction
    @Requires({ this.project && this.ext && this.extensionName })
    final void doTask()
    {
        verifyExtension( "${ this.extensionName } { .. }" )
        taskAction()
    }

    @Requires({ this.ext && description })
    abstract void verifyExtension( String description )

    @Requires({ ext && extensionName })
    abstract void taskAction()


    @Requires({ c != null })
    @Ensures({ result != null })
    final String s( Collection c ){ s( c.size()) }

    @Requires({ j > -1 })
    @Ensures({ result != null })
    final String s( Number j ){ j == 1 ? '' : 's' }


    /**
     * Verifies 'git' client is available.
     */
    final void verifyGitAvailable ()
    {
        final  gitVersion = gitExec( '--version', project.rootDir, false )
        assert gitVersion.contains( 'git version' ), \
               "'git' client is not available - 'git --version' returned [$gitVersion]"
    }

    /**
     * Executes the bash script specified.
     *
     * @param  bashScript bash script to execute
     * @return bash output
     */
    @Requires({ bashScript.file })
    @Ensures({ result != null })
    final String bashExec( File bashScript, File directory = null, boolean failOnError = true )
    {
        exec( 'bash', [ bashScript.canonicalPath ], directory, failOnError )
    }


   /**
    * Executes 'git' command specified.
    *
    * @param command     command to execute
    * @param directory   directory to execute the command in
    * @param failOnError whether execution should fail in case of an error
    *
    * @return command output
    */
    @Requires({ command && directory.directory })
    @Ensures({ result != null })
    final String gitExec( String command, File directory, boolean failOnError = true )
    {
        exec( 'git', command.tokenize(), directory, failOnError )
    }


    /**
     * Executes the command specified.
     *
     * @param command     command to execute
     * @param arguments   command arguments
     * @param directory   process working directory
     * @param failOnError whether execution should fail in case of an error
     *
     * @return process standard and error output
     */
    @Requires({ command && ( ! command.contains( ' ' )) && ( arguments != null ) })
    @Ensures({ result != null })
    final String exec( String       command,
                       List<String> arguments    = [],
                       File         directory    = null,
                       boolean      failOnError  = true )
    {
        final commandDescription = "[$command]${ arguments ? ' with arguments ' + arguments : '' }" +
                                   "${ directory ? ' in [' + directory.canonicalPath + ']' : '' }"

        log{ "Running $commandDescription" }

        final stdoutStream = logger.infoEnabled ? new LoggingOutputStream( ">> $command: ", logger, LogLevel.INFO ) :
                                                  new ByteArrayOutputStream()
        final stderrStream = logger.infoEnabled ? new LoggingOutputStream( ">> $command: ", logger, LogLevel.INFO ) :
                                                  new ByteArrayOutputStream()
        try
        {
            project.exec {
                ExecSpec spec ->
                spec.with {
                    executable( command )
                    if ( arguments ) { args( arguments )      }
                    if ( directory ) { workingDir = directory }
                    standardOutput = stdoutStream
                    errorOutput    = stderrStream
                }
            }
        }
        catch ( Throwable error )
        {
            final stdout = stdoutStream.toString().trim()
            final stderr = stderrStream.toString().trim()

            if ( failOnError )
            {
                throw new GradleException( "Failed to execute $commandDescription, stdout is [$stdout], stderr is [$stderr]",
                                           error )
            }

            if ( ! ( stdout || stderr )) { error.printStackTrace( new PrintStream( stderrStream, true )) }
        }

        stdoutStream.toString().trim() + stderrStream.toString().trim()
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
    @Ensures({ result != null })
    final List<File> files ( File         baseDirectory,
                             List<String> includePatterns    = null,
                             List<String> excludePatterns    = null,
                             boolean      isCaseSensitive    = true,
                             boolean      includeDirectories = false,
                             boolean      failIfNotFound     = true )
    {
        final scanner = new DirectoryScanner()

        scanner.with {
            basedir           = baseDirectory
            includes          = includePatterns as String[]
            excludes          = excludePatterns as String[]
            caseSensitive     = isCaseSensitive
            errorOnMissingDir = true
            followSymlinks    = false
            scan()
        }

        List<File> files = scanner.includedFiles.collect { new File( baseDirectory, it ) } +
                           ( includeDirectories ? scanner.includedDirectories.collect { new File( baseDirectory, it ) } :
                                                  [] )

        assert ( files || ( ! failIfNotFound )), \
               "No files are included by parent dir [$baseDirectory] and " +
               "include/exclude patterns ${ includePatterns ?: [] }/${ excludePatterns ?: [] }"

        assert files.every { it.file || it.directory }
        files
    }


    /**
     * Archives files specified.
     *
     * @param files files to archive
     * @return first file specified
     */
    @Requires({ files })
    @Ensures({ result })
    final File zip ( File ... files )
    {
        assert files.every { it && it.file }
        files.each { File f -> zip( project.file( "${ f.canonicalPath }.zip" )){ ant.zipfileset( file: f.canonicalPath ) }}
        files.first()
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
    final File zip ( File archive, Closure zipClosure )
    {
        project.delete( archive )
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
    @Requires({ archive && file && ( prefix != null ) })
    final void addFileToArchive ( File         archive,
                                  File         file,
                                  String       prefix,
                                  List<String> includes = null,
                                  List<String> excludes = null )
    {
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
    @Requires({ files && resources })
    final void checkResources( Collection<File> files, String ... resources )
    {
        final cl = new URLClassLoader( files*.toURI()*.toURL() as URL[] )
        resources.each { assert cl.getResource( it ), "No '$it' resource found in $files" }
    }


    @Requires({ dir })
    @Ensures({ ( result == dir ) && ( result.directory ) && ( ! result.list())})
    final File makeEmptyDirectory( File dir )
    {
        assert (( ! dir.exists()) || ( project.delete( dir ) && ( ! dir.exists())))
        project.mkdir( dir )
    }


    /**
     * Validates XML specified with Schema provided.
     *
     * @param xml    XML to validate
     * @param schema schema to validate with
     * @return       same XML instance
     * @throws       GradleException if validation fails
     */
    @Requires({ xml && schema })
    final String validateXml( String xml, String schema )
    {
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


    /**
     * Retrieves all appearances of the first capturing group of the pattern specified in a String or empty list if not found.
     */
    @Requires({ s && p })
    @Ensures({ result != null })
    final List<String> findAll( String s, Pattern p ){ s.findAll ( p ) { it[ 1 ] }}


    /**
     * Retrieves first appearance of the first capturing group of the pattern specified in a String or null if not found.
     */
    @Requires({ s && p })
    final String find( String s, Pattern p ){ s.find ( p ) { it[ 1 ] }}


    /**
     * Logs message returned by the closure provided.
     *
     * @param logLevel           message log level
     * @param error              error thrown
     * @param logMessageCallback closure returning message text
     */
    @Requires({ logger && logLevel && logMessageCallback })
    final String log( LogLevel logLevel = LogLevel.INFO, Throwable error = null, Closure logMessageCallback )
    {
        String logText = null

        if ( logger.isEnabled( logLevel ))
        {
            logText = logMessageCallback()
            assert logText

            if ( error ) { logger.log( logLevel, logText, error )}
            else         { logger.log( logLevel, logText )}
        }

        logText
    }


    @Requires({ message })
    final failOrWarn( boolean shouldFail, String message, Throwable error = null )
    {
        if ( shouldFail )
        {
            if ( error ) { throw new GradleException( message, error )}
            else         { throw new GradleException( message )}
        }
        else
        {
            log( LogLevel.WARN, error ){ message }
        }
    }
}
