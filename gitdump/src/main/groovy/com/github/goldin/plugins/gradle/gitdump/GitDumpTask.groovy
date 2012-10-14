package com.github.goldin.plugins.gradle.gitdump

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * {@link GitDumpPlugin} task.
 */
class GitDumpTask extends BaseTask
{
    private GitDumpExtension ext () { extension ( GitDumpPlugin.EXTENSION_NAME, GitDumpExtension ) }


    @Override
    void taskAction ( )
    {
        final ext = verifyAndUpdateExtension()
        for ( repoUrl in ext.urls )
        {
            final  projectName = repoUrl.find( /\/([^\/]+).git$/ ){ it[1] }
            assert projectName, "Failed to match a project name in [$repoUrl]"

            final repoDirectory = cloneRepository( repoUrl, projectName )

            if ( ! ext.singleArchive )
            {
                archive( repoDirectory, projectName, true, ext.singleBackupMaxSize  )
            }
        }

        if ( ext.singleArchive )
        {
            archive( ext.outputDirectory, ext.singleArchiveName, false, ext.totalBackupMaxSize )
        }
    }


    @Ensures({ result })
    GitDumpExtension verifyAndUpdateExtension ()
    {
        final ext = ext()
        final description = "${ GitDumpPlugin.EXTENSION_NAME } { .. }"

        assert ext.urls, "List of Git URLs is not defined in $description"
        ext.urls.each { assert it.endsWith( '.git' ), "[$it] is not a Git repository URL, should end with '.git'" }
        assert ext.singleArchiveName,       "'singleArchiveName' should be defined in $description"
        assert ext.singleBackupMaxSize > 0, "'singleBackupMaxSize' should be positive in $description"
        assert ext.totalBackupMaxSize  > 0, "'totalBackupMaxSize' should be positive in $description"

        ext.outputDirectory = makeEmptyDirectory( ext.outputDirectory?: new File( project.buildDir, 'gitdump' ))
        ext
    }


    @Requires({ dir })
    @Ensures({ ( result == dir ) && ( result.directory ) })
    File makeEmptyDirectory( File dir )
    {
        project.delete( dir )
        assert ( ! dir.directory )

        project.mkdir( dir )
        assert ( dir.directory && ( ! dir.list()))
        dir
    }


    @Requires({ repoUrl && projectName })
    @Ensures({ result.directory })
    File cloneRepository ( String repoUrl, String projectName )
    {
        final ext       = ext()
        final directory = makeEmptyDirectory( new File( ext.outputDirectory, projectName ))
        final command   = [ 'git', 'clone', '--bare', repoUrl, directory.canonicalPath ]

        logger.info( "Running $command .." )
        project.exec { executable( command.head()); args( command.tail()) }
        logger.info( 'Done' )

        assert directory.list(), "[$directory.canonicalPath] contains no files"
        logger.info( "[$repoUrl] cloned to [$directory.canonicalPath]" )
        directory
    }


    @Requires({ directory.directory && directory.list() && archiveBaseName })
    @Ensures({ result.file })
    File archive( File directory, String archiveBaseName, boolean deleteDirectory, long maxSizeLimit )
    {
        final ext     = ext()
        final archive = new File( ext.outputDirectory, "${ archiveBaseName }.${ ext.useZip ? 'zip' : 'tar.gz' }" )
        project.delete( archive )
        assert ( ! archive.file )

        if ( ext.useZip )
        {
            ant.zip( destfile        : archive,
                     basedir         : directory,
                     whenempty       : 'fail',
                     level           : 9,
                     defaultexcludes : 'no' )
        }
        else
        {
            ant.tar( destfile    : archive,
                     basedir     : directory,
                     compression : 'gzip' )
        }

        assert ( archive.file && archive.length())
        assert ( archive.length() < maxSizeLimit ), "[$archive.canonicalPath] size [${ archive.length()}] is larger than [$maxSizeLimit] bytes"
        logger.info( "[$directory.canonicalPath] archived to [$archive.canonicalPath]" )

        if ( deleteDirectory )
        {
            project.delete( directory )
        }

        archive
    }
}
