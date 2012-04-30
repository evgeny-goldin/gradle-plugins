package com.github.goldin.plugins.gradle.util

import org.apache.tools.ant.DirectoryScanner
import org.gcontracts.annotations.Requires
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Base helper task class to be extended by other tasks
 */
abstract class BaseTask extends DefaultTask
{
    File   rootDir
    String version
    Jar    jarTask


    @TaskAction
    @Requires({ project.rootDir && project.group && project.name && project.version })
    def doTask()
    {
        this.rootDir = project.rootDir
        this.group   = project.group
        this.name    = project.name
        this.version = project.version
        this.jarTask = ( Jar ) project.tasks[ 'jar' ]
        taskAction()
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
     * Should be implemented by task.
     * Called after all fields are initialized.
     */
    abstract void taskAction()
}
