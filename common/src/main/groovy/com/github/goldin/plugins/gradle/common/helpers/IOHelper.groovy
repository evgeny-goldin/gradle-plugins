package com.github.goldin.plugins.gradle.common.helpers

import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.HttpResponse
import com.github.goldin.plugins.gradle.common.LoggingOutputStream
import groovy.text.SimpleTemplateEngine
import org.apache.tools.ant.DirectoryScanner
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.process.ExecSpec


final class IOHelper extends BaseHelper<Object>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    IOHelper ( Project project, BaseTask task, Object ext ){ super( project, task, ext )}


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
     String gitExec( String command, File directory = project.rootDir, boolean failOnError = true )
     {
         exec( 'git', command.tokenize(), directory, failOnError )
     }


     /**
      * Executes the command specified.
      *
      * @param command       command to execute
      * @param arguments     command arguments
      * @param directory     process working directory
      * @param failOnError   whether execution should fail in case of an error
      * @param useGradleExec whether Gradle (true) or Ant (false) exec is used
      * @param logLevel      log level to use for logging command output
      *
      * @return process standard and error output
      */
     @Requires({ command && ( ! command.contains( ' ' )) && ( arguments != null ) && logLevel })
     @Ensures ({ result != null })
     String exec( String       command,
                  List<String> arguments     = [],
                  File         directory     = project.rootDir,
                  boolean      failOnError   = true,
                  boolean      useGradleExec = true,
                  LogLevel     logLevel      = LogLevel.INFO )
     {
         final commandDescription = "[$command]${ arguments ? ' with arguments ' + arguments : '' }" +
                                    "${ directory ? ' in directory [' + directory.canonicalPath + ']' : '' }"

         log( logLevel ) { "Running $commandDescription" }

         OutputStream stdoutStream    = null
         OutputStream stderrStream    = null
         final        newOutputStream = { new LoggingOutputStream( ">> $command: ", logger, logLevel ) }

         try
         {
             if ( useGradleExec )
             {
                 stdoutStream = logger.isEnabled( logLevel ) ? newOutputStream() : new ByteArrayOutputStream()
                 stderrStream = logger.isEnabled( logLevel ) ? newOutputStream() : new ByteArrayOutputStream()

                 project.exec({ ExecSpec spec -> spec.with {
                     executable( command )
                     if ( arguments ) { args( arguments ) }
                     if ( directory ) { workingDir = directory }
                     standardOutput = stdoutStream
                     errorOutput    = stderrStream
                 }})
             }
             else
             {
                 ant.exec([ executable  : command,
                                 failonerror : failOnError ] + ( directory ? [ dir : directory ] : [:] )){
                     arg (  line        : arguments.join ( ' ' ))
                 }
             }
         }
         catch ( Throwable error )
         {
             final stdout = ( stdoutStream instanceof ByteArrayOutputStream ? stdoutStream.toString().trim() : '' )
             final stderr = ( stderrStream instanceof ByteArrayOutputStream ? stderrStream.toString().trim() : '' )

             if ( failOnError )
             {
                 throw new GradleException( "Failed to execute $commandDescription, stdout is [$stdout], stderr is [$stderr]",
                                            error )
             }
         }

         useGradleExec ? stdoutStream.toString().trim() + stderrStream.toString().trim() :
                         ''
     }


     /**
      * {@link org.gradle.api.Project#file(java.lang.Object)} wrapper validating the file created exists already.
      */
     @Requires({ path })
     @Ensures ({ result.file })
     File checkFile( String path )
     {
         final  f = project.file( path ).canonicalFile
         assert f.file, "File [$f] is not available"
         f
     }


     /**
      * {@link org.gradle.api.Project#file(java.lang.Object)} wrapper validating the file created is directory.
      */
     @Requires({ path })
     @Ensures ({ result.directory })
     File checkDirectory( String path )
     {
         final  f = project.file( path ).canonicalFile
         assert f.directory, "Directory [$f] is not available"
         f
     }


     /**
      * {@code File.write()} wrapper.
      */
     @Requires({ file && ( content != null ) && encoding })
     @Ensures ({ result == file })
     File write ( File file, String content, String encoding = 'UTF-8' )
     {
         assert ( ! file.file ) || project.delete( file ), "Unable to delete [${ file.canonicalPath }]"
         assert file.parentFile.with { directory || mkdirs() }, "Unable to mkdir [${ file.parentFile.canonicalPath }]"

         file.write( content, encoding )
         assert ( file.file && ( file.size() >= content.size()))
         file
     }


     /**
      * Renders template provided using the binding mapping specified.
      *
      * @param template template to render
      * @param binding binding to use when template is rendered
      * @return template rendered
      */
     @Requires({ template && ( binding != null ) })
     @Ensures ({ result })
     String renderTemplate( String template, Map<String,?> binding )
     {
         new SimpleTemplateEngine().createTemplate( template ).make( binding ).toString()
     }


     /**
      * {@link org.gradle.api.Project#delete(java.lang.Object...)} wrapper logging the files being deleted
      * and verifying delete operation was successful.
      *
      * @param  mustDelete whether all file have to be deleted,
      *                    if false then failing to delete any file will cause a warning and not an error
      * @param  files      files to delete
      * @return files specified
      */
     @SuppressWarnings([ 'GroovyOverlyNestedMethod' ])
     @Requires({ files != null })
     void delete( boolean mustDelete = true, Object ... files )
     {
         if ( ! files ) { return }

         for ( file in files.grep().collect{ project.file( it ) })
         {   /**
              * Files can be deleted by previous loop iterations
              */
             if ( file.exists())
             {
                 log { "Deleting [$file.canonicalPath]" }

                 try { project.delete( file ) }
                 catch ( Throwable ignored )
                 {   // http://issues.gradle.org/browse/GRADLE-2581 or locked
                     if ( isWindows || isLinux || isMac )
                     {
                         if ( isWindows )
                         {
                             exec( 'rmdir', [ '/s', '/q', file.canonicalPath ], null, false )
                             exec( 'del',   [ '/f', '/q', file.canonicalPath ], null, false )
                         }
                         else if ( isLinux || isMac )
                         {
                             exec( 'rm', [ '-rf', file.canonicalPath ], null, false )
                         }

                         if ( file.exists()){ failOrWarn( mustDelete, "Failed to natively delete [$file.canonicalPath]" )}
                     }
                 }
             }

             if ( file.exists()){ failOrWarn( mustDelete, "Failed to delete [$file.canonicalPath]" )}
         }
     }


     /**
      * Retrieves files (and directories, if required) given base directory and inclusion/exclusion patterns.
      * Symbolic links are not followed.
      *
      * @param baseDirectory      files base directory
      * @param includePatterns    comma-separated patterns to use for including files, all files are included if null
      * @param excludePatterns    comma-separated patterns to use for excluding files, no files are excluded if null
      * @param isCaseSensitive    whether or not include and exclude patterns are matched in a case sensitive way
      * @param includeDirectories whether directories included should be returned as well
      * @param failIfNotFound     whether execution should fail if no files were found
      *
      * @return files under base directory specified passing an inclusion/exclusion patterns
      */
     @Requires({ baseDirectory.directory })
     @Ensures({ result != null })
     List<File> files ( File    baseDirectory,
                        String  includePatterns    = null,
                        String  excludePatterns    = null,
                        boolean isCaseSensitive    = true,
                        boolean includeDirectories = false,
                        boolean failIfNotFound     = true )
     {
         final split   = { String s -> s ? s.split( ',' )*.trim().grep() as String[] : null }
         final scanner = new DirectoryScanner()

         scanner.with {
             basedir           = baseDirectory
             includes          = split( includePatterns )
             excludes          = split( excludePatterns )
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
                "include/exclude patterns ${ includePatterns ?: '' }/${ excludePatterns ?: '' }"

         assert files.every { it.file || ( it.directory && includeDirectories ) }
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
     File zip ( File ... files )
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
     File zip ( File archive, Closure zipClosure )
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
     void addFilesToArchive ( File             archive,
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
     void addFileToArchive ( File         archive,
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
     * {@code this.class.classLoader.getResourceAsStream} wrapper.
     *
     * @param resourcePath resource to load
     * @return resource {@code InputStream}
     */
    @Requires({ resourcePath })
    @Ensures ({ result })
    InputStream getResource ( String resourcePath )
    {
        final  inputStream = this.class.classLoader.getResourceAsStream( resourcePath.startsWith( '/' ) ? resourcePath.substring( 1 ) : resourcePath )
        assert inputStream, "Unable to load resource [$resourcePath]. If you believe there's no mistake then consider restarting the Gradle daemon"
        inputStream
    }


    /**
     * {@code this.class.classLoader.getResourceAsStream.getText} wrapper.
     *
     * @param resourcePath resource to load
     * @param charset to use when reading resource text content
     * @return resource text
     */
    @Requires({ resourcePath && ( replacements != null ) && charset })
    @Ensures ({ ! (( result == null ) || result.contains( '@{' )) })
    String getResourceText( String resourcePath, Map<String, String> replacements = [:], String charset = 'UTF-8' )
    {
        replacements.inject( getResource( resourcePath ).getText( charset )){
            String text, String pattern, String replacement ->
            text.replace( "@{$pattern}", replacement )
        }
    }


    /**
     * Invokes an HTTP request using the data provided.
     *
     * @param url            url to send the request to
     * @param method         HTTP method to use: 'GET' or 'HEAD'
     * @param headers        HTTP headers to send
     * @param connectTimeout connection timeout to set (in milliseconds)
     * @param readTimeout    connection read timeout to set (in milliseconds)
     * @param failOnError    whether execution should fail if sending request fails
     * @param logError       whether an error thrown should be logged
     * @param username       username to authenticate with using Basic authentication
     * @param password       userame to authenticate with using Basic authentication
     * @param isReadContent  closure returning boolean value of whether or not content should be read,
     *                       passed {@link com.github.goldin.plugins.gradle.common.HttpResponse} when called
     * @param data           data to send if method is POST or PUT
     *
     * @return http response object
     */
    @Requires({ url && method && ( headers != null ) && ( connectTimeout > -1 ) && ( readTimeout > -1 ) })
    @Ensures ({ result })
    @SuppressWarnings([ 'GroovyGetterCallCanBePropertyAccess', 'JavaStylePropertiesInvocation', 'GroovyMethodParameterCount', 'GroovyAssignmentToMethodParameter' ])
    HttpResponse httpRequest( String              url,
                              String              method         = 'GET',
                              Map<String, String> headers        = [:],
                              int                 connectTimeout = 0,
                              int                 readTimeout    = 0,
                              boolean             failOnError    = true,
                              boolean             logError       = true,
                              String              username       = null,
                              String              password       = null,
                              Closure             isReadContent  = null,
                              byte[]              data           = null )
    {
        assert url.with { startsWith( 'http://' ) || startsWith( 'https://' )}, "[$url] - only 'http[s]://' URLs are supported"
        assert ( ! data ) || ( method == 'POST' ) || ( method == 'PUT' ), "HTTP [$method] request - data can only be sent for POST and PUT requests"

        final time     = System.currentTimeMillis()
        final response = new HttpResponse( url, method )

        headers += [ 'Accept-Encoding': 'gzip,deflate' ]
        headers += (( username && password ) ?
                   [ 'Authorization'  : 'Basic ' + "$username:$password".getBytes( 'UTF-8' ).encodeBase64().toString() ] :
                   [:] )
        try
        {
            final connection          = url.replace( ' ' as char, '+' as char ).toURL().openConnection() as HttpURLConnection
            response.connection       = connection
            connection.requestMethod  = method
            connection.connectTimeout = connectTimeout
            connection.readTimeout    = readTimeout

            headers.each { String name, String value -> connection.setRequestProperty( name, value )}

            if ( data )
            {
                connection.doOutput = true
                connection.outputStream.write( data )
            }

            response.inputStream = connection.inputStream
            response.actualUrl   = connection.getURL().toString()
        }
        catch ( Throwable error )
        {
            if ( failOnError ) { throw error }
            response.errorStream = response.connection.errorStream
            if ( logError    ) { log{ "Connecting to [$url], method [$method], headers $headers resulted in '$error'" }}
        }

        response.statusCode = HttpResponse.statusCode( response )

        if (( isReadContent == null ) || isReadContent( response ))
        {
            final inputStream = ( response.inputStream ?: response.errorStream )
            if ( inputStream )
            {
                response.data = inputStream.bytes
            }
            response.inputStream?.close()
            response.errorStream?.close()
        }

        response.timeMillis = System.currentTimeMillis() - time
        response
    }
}
