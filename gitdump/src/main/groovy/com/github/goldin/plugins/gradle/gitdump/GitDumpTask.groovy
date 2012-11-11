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
    private getLastCommit( File projectDirectory ){ gitExec( 'log -1 --format=format:%H', projectDirectory ) }

    @Override
    void taskAction ( )
    {
        final ext = verifyAndUpdateExtension()
        verifyGitIsAvailable()
        initAboutFile()

        for ( repoUrl in ext.urls )
        {
            final  projectName  = find( repoUrl, ext.gitProjectNamePattern )
            assert projectName, "Failed to match a project name in [$repoUrl]"

            final repoDirectory = cloneRepository( repoUrl, projectName )

            if ( ! ext.singleArchive )
            {
                archive( repoDirectory, projectName, true, ext.singleBackupMaxSize  )
            }
        }

        if ( ext.singleArchive )
        {
            final archive = archive( ext.outputDirectory, ext.singleArchiveName, false, ext.totalBackupMaxSize )
            project.delete( ext.outputDirectory.listFiles().findAll{ it.name != archive.name })
        }
    }


    @Ensures({ result })
    GitDumpExtension verifyAndUpdateExtension ()
    {
        final ext = ext()
        final description = "${ GitDumpPlugin.EXTENSION_NAME } { .. }"

        assert ext.singleArchiveName,       "'singleArchiveName' should be defined in $description"
        assert ext.gitProjectNamePattern,   "'gitProjectNamePattern' should be defined in $description"
        assert ext.gitUrlWithCommitPattern, "'gitUrlWithCommitPattern' should be defined in $description"
        assert ext.singleBackupMaxSize > 0, "'singleBackupMaxSize' should be positive in $description"
        assert ext.totalBackupMaxSize  > 0, "'totalBackupMaxSize' should be positive in $description"

        ext.urls = ext.urls?.grep()?.toSet()?.sort()
        assert ext.urls, "List of Git URLs should be specifed in $description"
        ext.urls.each { assert ( it =~ ext.gitProjectNamePattern ), "[$it] is not a Git repository URL, doesn't match [$ext.gitProjectNamePattern]" }

        ext.outputDirectory = makeEmptyDirectory( ext.outputDirectory?: new File( project.buildDir, 'gitdump' ))
        ext.aboutFile       = ( ext.addAbout ? new File( ext.outputDirectory, 'about.txt' ) : null )

        if ( logger.infoEnabled )
        {
            logger.info( "Dumping Git repositories $ext.urls to [$ext.outputDirectory.canonicalPath]" )
        }

        ext
    }

    void verifyGitIsAvailable ( )
    {
        final  gitVersion = gitExec( '--version', project.rootDir, false )
        assert gitVersion.contains( 'git version' ), \
               "'git' client is not available - 'git --version' returned [$gitVersion]"
    }


    @Requires({ repoUrl && projectName })
    @Ensures({ result.directory })
    File cloneRepository ( String repoUrl, String projectName )
    {
        final   ext             = ext()
        String  checkoutId      = null
        String  lastCommit      = null
        boolean bareClone       = ext.bareClone
        final   targetDirectory = makeEmptyDirectory( new File( ext.outputDirectory, projectName ))
        final   dotGit          = new File( targetDirectory, '.git' )


        if ( repoUrl ==~ ext.gitUrlWithCommitPattern )
        {
            ( repoUrl, checkoutId ) = repoUrl.findAll( ext.gitUrlWithCommitPattern ){ it[ 1 .. 2 ] }[ 0 ]
            assert ( repoUrl && checkoutId ), "Failed to match repo URL and commit in [$repoUrl]"
            bareClone = false
        }

        exec( 'git',
              (( List<String> ) [ 'clone', *ext.cloneFlags, ( bareClone ? '--bare' : '' ), repoUrl, targetDirectory.canonicalPath ].grep()),
              project.rootDir )

        if ( ! bareClone ){ assert dotGit.directory }

        if ( checkoutId )
        {
            gitExec( "checkout $checkoutId", targetDirectory )
            lastCommit = getLastCommit( targetDirectory )
            assert ( project.delete( dotGit ) && ( ! dotGit.directory ))
        }
        else
        {
            lastCommit = getLastCommit( targetDirectory )

            if ( ext.runAggressiveGitGc )
            {
                project.delete( new File( targetDirectory, ( bareClone ? 'hooks' : '.git/hooks' )))
                gitExec( 'reflog expire --all --expire=1.minute', targetDirectory )
            }

            if ( ext.runGitGc )
            {
                gitExec( 'fsck --unreachable --strict', targetDirectory )
                gitExec( 'prune',                       targetDirectory )
                gitExec( 'gc',                          targetDirectory )
            }

            assert targetDirectory.list(), "[$targetDirectory.canonicalPath] contains no files"
        }

        if ( logger.infoEnabled )
        {
            logger.info( "[$repoUrl${ checkoutId ? ':' + checkoutId : '' }] cloned into [$targetDirectory.canonicalPath]" )
        }

        updateAboutFile( projectName, repoUrl, lastCommit )
        targetDirectory
    }


    void initAboutFile ()
    {
        final ext = ext()
        if ( ext.aboutFile )
        {
            final line = ( '-' * ( startTimeFormatted.length() + 2 ))
            ext.aboutFile.write( "$line\n $startTimeFormatted\n$line" )
        }
    }


    @Requires({ projectName && repoUrl && lastCommit })
    void updateAboutFile ( String projectName, String repoUrl, String lastCommit )
    {
        final ext = ext()
        if ( ext.aboutFile )
        {
            ext.aboutFile.append( """\n
[$projectName]:
 * Repo        - [$repoUrl]
 * Last commit - [$lastCommit]""" )
        }
    }


    @Requires({ directory.directory && archiveBaseName })
    @Ensures({ result.file })
    File archive( File directory, String archiveBaseName, boolean deleteDirectory, long maxSizeLimit )
    {
        final ext     = ext()
        final archive = new File( ext.outputDirectory, "${ archiveBaseName }.${ ext.useZip ? 'zip' : 'tar.gz' }" )

        assert (( ! archive.file ) || ( project.delete( archive ) && ( ! archive.file )))

        if ( ext.useZip )
        {
            ant.zip( destfile        : archive,
                     basedir         : directory,
                     whenempty       : 'fail',
                     defaultexcludes : 'no',
                     level           : 9 )
        }
        else
        {
            ant.tar( destfile    : archive,
                     basedir     : directory,
                     compression : 'gzip' )
        }

        final size = archive.length()
        assert ( archive.file && size )
        assert ( size < maxSizeLimit ), \
               "[$archive.canonicalPath] size is [$size] byte${ s( size )}, it is larger than limit of [$maxSizeLimit] byte${ s( maxSizeLimit )}"

        if ( logger.infoEnabled )
        {
            logger.info( "[$directory.canonicalPath] archived to [$archive.canonicalPath]" )
        }

        if ( deleteDirectory )
        {
            assert ( project.delete( directory ) && ( ! directory.directory ))
        }

        archive
    }
}
