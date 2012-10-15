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
    private gitExec( List<String> commands, File directory = null ){ exec( 'git', commands, directory )}
    private gitExec( String command,        File directory = null ){ exec( 'git', command,  directory )}


    @Override
    void taskAction ( )
    {
        final ext = verifyAndUpdateExtension()
        verifyGitAvailable()

        if ( ext.aboutFile )
        {
            ext.aboutFile.write( dateFormatter.format( startTime ))
        }

        for ( repoUrl in ext.urls )
        {
            final  projectName  = repoUrl.find( ext.gitProjectNamePattern ){ it[ 1 ] }
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

        assert ext.urls, "List of Git URLs is not defined in $description"
        ext.urls.each { assert it.endsWith( '.git' ), "[$it] is not a Git repository URL, should end with '.git'" }
        assert ext.singleArchiveName,       "'singleArchiveName' should be defined in $description"
        assert ext.gitProjectNamePattern,   "'gitProjectNamePattern' should be defined in $description"
        assert ext.singleBackupMaxSize > 0, "'singleBackupMaxSize' should be positive in $description"
        assert ext.totalBackupMaxSize  > 0, "'totalBackupMaxSize' should be positive in $description"

        ext.outputDirectory = makeEmptyDirectory( ext.outputDirectory?: new File( project.buildDir, 'gitdump' ))
        ext.aboutFile       = ( ext.addAbout ? new File( ext.outputDirectory, 'about.txt' ) : null )
        ext
    }


    void verifyGitAvailable ( )
    {
        assert gitExec( '--version' ).contains( 'git version' ), "'git' client is not available :^("
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
        final ext             = ext()
        final targetDirectory = makeEmptyDirectory( new File( ext.outputDirectory, projectName ))
        final arguments       = ( List<String> ) [ 'clone', *ext.cloneFlags, ext.bareClone ? '--bare' : '',
                                                   repoUrl, targetDirectory.canonicalPath ].grep()
        gitExec( arguments )

        if ( ext.runAggressiveGitGc )
        {
            project.delete( new File( targetDirectory, ext.bareClone ? 'hooks' : '.git/hooks' ))
            gitExec( 'reflog expire --all --expire=1.minute', targetDirectory )
        }

        if ( ext.runGitGc )
        {
            gitExec( 'fsck --unreachable --strict', targetDirectory )
            gitExec( 'prune', targetDirectory )
            gitExec( 'gc', targetDirectory )
        }

        assert targetDirectory.list(), "[$targetDirectory.canonicalPath] contains no files"
        logger.info( "[$repoUrl] cloned into [$targetDirectory.canonicalPath]" )

        if ( ext.addAbout )
        {
            ext.aboutFile.append( """
[$projectName]:
 * Repo   - [$repoUrl]
 * Commit - [${ gitExec( 'log -1 --format=format:%H' )}]""" )
        }

        targetDirectory
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

        logger.info( "[$directory.canonicalPath] archived to [$archive.canonicalPath]" )

        if ( deleteDirectory )
        {
            project.delete( directory )
        }

        archive
    }
}
