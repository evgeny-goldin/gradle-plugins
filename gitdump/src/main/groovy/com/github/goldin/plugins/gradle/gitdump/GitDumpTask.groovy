package com.github.goldin.plugins.gradle.gitdump

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * {@link GitDumpPlugin} task.
 */
class GitDumpTask extends BaseTask<GitDumpExtension>
{
    @Ensures({ result })
    private String lastCommitAllBranches   ( File projectDirectory ){ gitExec( 'log -1 --all --format=format:%H', projectDirectory )}

    @Ensures({ result })
    private String lastCommitCurrentBranch ( File projectDirectory ){ gitExec( 'log -1 --format=format:%H', projectDirectory ) }


    @Override
    void verifyUpdateExtension ( String description )
    {
        assert ext.singleArchiveName,       "'singleArchiveName' should be defined in $description"
        assert ext.gitProjectNamePattern,   "'gitProjectNamePattern' should be defined in $description"
        assert ext.gitUrlWithCommitPattern, "'gitUrlWithCommitPattern' should be defined in $description"
        assert ext.singleBackupMinSize > 0, "'singleBackupMinSize' should be positive in $description"
        assert ext.totalBackupMinSize  > 0, "'totalBackupMinSize' should be positive in $description"
        assert ext.singleBackupMaxSize > 0, "'singleBackupMaxSize' should be positive in $description"
        assert ext.totalBackupMaxSize  > 0, "'totalBackupMaxSize' should be positive in $description"

        assert ext.singleBackupMinSize <= ext.singleBackupMaxSize
        assert ext.totalBackupMinSize  <= ext.totalBackupMaxSize

        ext.urls = ext.urls?.grep()?.toSet()?.sort()
        assert ext.urls, "List of Git URLs should be specifed in $description"
        ext.urls.each { assert ( it =~ ext.gitProjectNamePattern ), "[$it] is not a Git repository URL, doesn't match [$ext.gitProjectNamePattern]" }

        ext.outputDirectory = makeEmptyDirectory( ext.outputDirectory?: new File( project.buildDir, 'gitdump' ))
        ext.aboutFile       = ( ext.addAbout ? new File( ext.outputDirectory, 'about.txt' ) : null )
    }


    @Override
    void taskAction ( )
    {
        log { "Dumping Git repositories $ext.urls to [$ext.outputDirectory.canonicalPath]" }

        verifyGitAvailable()
        initAboutFile()

        for ( repoUrl in ext.urls )
        {
            final  projectName  = find( repoUrl, ext.gitProjectNamePattern )
            assert projectName, "Failed to match a project name in [$repoUrl]"

            final repoDirectory = cloneRepository( repoUrl, projectName )

            if ( ! ext.singleArchive )
            {
                archive( repoDirectory, projectName, true, ext.singleBackupMinSize, ext.singleBackupMaxSize )
            }
        }

        if ( ext.singleArchive )
        {
            final archive = archive( ext.outputDirectory, ext.singleArchiveName, false, ext.totalBackupMinSize, ext.totalBackupMaxSize )
            delete( ext.outputDirectory.listFiles().findAll{ it.name != archive.name } as File[] )
        }
    }


    @Requires({ repoUrl && projectName })
    @Ensures({ result.directory })
    File cloneRepository ( String repoUrl, String projectName )
    {
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

        log{ "[$repoUrl] cloned into [$targetDirectory.canonicalPath]" }

        if ( ! bareClone ){ assert dotGit.directory }

        if ( checkoutId )
        {
            checkoutId = ( checkoutId == '<last>' ) ? lastCommitAllBranches( targetDirectory ) : checkoutId
            gitExec( "checkout -q $checkoutId", targetDirectory )
            lastCommit = lastCommitCurrentBranch( targetDirectory )
            delete( dotGit )
        }
        else
        {
            lastCommit = lastCommitCurrentBranch( targetDirectory )

            if ( ext.runAggressiveGitGc )
            {
                delete( new File( targetDirectory, ( bareClone ? 'hooks' : '.git/hooks' )))
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

        updateAboutFile( projectName, repoUrl, lastCommit )
        targetDirectory
    }



    void initAboutFile ()
    {
        if ( ext.aboutFile )
        {
            final line = ( '-' * ( startTimeFormatted.length() + 2 ))
            ext.aboutFile.write( "$line\n $startTimeFormatted\n$line" )
        }
    }


    @Requires({ projectName && repoUrl.endsWith( '.git' ) && commit ==~ /\w{40}/ })
    void updateAboutFile ( String projectName, String repoUrl, String commit )
    {
        if ( ext.aboutFile )
        {
            ext.aboutFile.append( """\n
[$projectName]:
 * Repo   - [$repoUrl]
 * Commit - [$commit]""" )
        }
    }


    @Requires({ directory.directory && archiveBaseName && ( minSizeLimit > 0 ) && ( maxSizeLimit > 0 ) && ( minSizeLimit <= maxSizeLimit ) })
    @Ensures({ result.file })
    File archive( File directory, String archiveBaseName, boolean deleteDirectory, long minSizeLimit, long maxSizeLimit )
    {
        final   archive = new File( ext.outputDirectory, "${ archiveBaseName }.${ ext.useZip ? 'zip' : 'tar.gz' }" )
        delete( archive )

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
        assert ( size >= minSizeLimit ), \
               "[$archive.canonicalPath] size is [$size] byte${ s( size )}, it is smaller than limit of [$minSizeLimit] byte${ s( minSizeLimit )}"
        assert ( size <= maxSizeLimit ), \
               "[$archive.canonicalPath] size is [$size] byte${ s( size )}, it is larger than limit of [$maxSizeLimit] byte${ s( maxSizeLimit )}"

        log { "[$directory.canonicalPath] archived to [$archive.canonicalPath]" }

        if ( deleteDirectory ){ delete( directory )}
        archive
    }
}
