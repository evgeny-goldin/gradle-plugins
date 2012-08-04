package com.github.goldin.plugins.gradle.common

import org.apache.tools.ant.DirectoryScanner
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction


/**
 * Base helper task class to be extended by other tasks
 */
abstract class BaseTask extends DefaultTask
{
    File   rootDir
    String version


    @TaskAction
    @Requires({ project.rootDir && project.version })
    def doTask()
    {
        this.rootDir = project.rootDir
        this.version = project.version
        taskAction()
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
    public <T> T extension( String extensionName, Class<T> extensionType )
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
    List<File> files ( File         baseDirectory,
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
    File delete ( File ... files )
    {
        for ( file in files.grep())
        {
            if      ( file.file      ){ assert file.delete(),    "Failed to delete file [$file.canonicalPath]"      }
            else if ( file.directory ){ assert file.deleteDir(), "Failed to delete directory [$file.canonicalPath]" }
        }

        (( files.size() > 0 ) ? files[ 0 ] : null )
    }


    /**
     * Should be implemented by task.
     * Called after all fields are initialized.
     */
    abstract void taskAction()
}
