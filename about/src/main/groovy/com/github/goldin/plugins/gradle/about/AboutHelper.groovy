package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project
import org.gradle.api.plugins.ProjectReportsPlugin
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import org.gradle.api.tasks.diagnostics.internal.DependencyReportRenderer


/**
 * {@link AboutTask} helper class.
 */
class AboutHelper extends BaseHelper<AboutExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    AboutHelper ( Project project, BaseTask task, AboutExtension ext ){ super( project, task, ext )}

    private static final String SEPARATOR = '|==============================================================================='


    @Requires({ ( s != null ) && prefix })
    @Ensures({ result != null })
    private String padLines ( String s, String prefix )
    {
        final lines = s.readLines()
        ( lines ? ( lines[ 0 ] + (( lines.size() > 1 ) ? '\n' + lines[ 1 .. -1 ].collect { '|' + ( ' ' * prefix.size()) + it }.join( '\n' ) :
                                                         '' )) :
                  '' )
    }


    @Requires({ map })
    @Ensures({ result })
    private String sort ( Map<String,?> map )
    {
        final keys       = map.keySet()
        final keyPadSize = keys*.size().max() + 3
        keys.sort().
             collect { String key -> "[$key]".padRight( keyPadSize ) + ":[${ map[ key ].toString() }]" }.
             join( '\n' )
    }


    @Ensures({ result })
    String jenkinsContent()
    {
        // https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project

        """
        $SEPARATOR
        | Jenkins Info
        $SEPARATOR
        | Server         : [${ systemEnv.JENKINS_URL }]
        | Job            : [${ systemEnv.JENKINS_URL }job/${ systemEnv.JOB_NAME }/${ systemEnv.BUILD_NUMBER }/]
        | Log            : [${ systemEnv.JENKINS_URL }job/${ systemEnv.JOB_NAME }/${ systemEnv.BUILD_NUMBER }/console]"""
    }


    @Ensures({ result })
    String hudsonContent()
    {
        // http://weblogs.java.net/blog/johnsmart/archive/2008/03/using_hudson_en.html

        """
        $SEPARATOR
        | Hudson Info
        $SEPARATOR
        | Server         : [${ systemEnv.HUDSON_URL }]
        | Job            : [${ systemEnv.HUDSON_URL }job/${ systemEnv.JOB_NAME }/${ systemEnv.BUILD_NUMBER }/]
        | Log            : [${ systemEnv.HUDSON_URL }job/${ systemEnv.JOB_NAME }/${ systemEnv.BUILD_NUMBER }/console]"""
    }


    @Requires({ teamcityProperties })
    @Ensures({ result })
    String teamcityContent()
    {
        final missingUrlMessage = 'Define \'TEAMCITY_URL\' environment variable, like TEAMCITY_URL=http://teamcity.jetbrains.com/'

        // http://confluence.jetbrains.net/display/TCD7/Predefined+Build+Parameters

        """
        $SEPARATOR
        | TeamCity Info
        $SEPARATOR
        | Version        : [${ teamcityProperty( 'teamcity.version' )}]
        | Server         : [${ teamCityUrl      ?: missingUrlMessage }]
        | Job            : [${ teamCityBuildUrl ?: missingUrlMessage }]
        | Log            : [${ teamCityBuildUrl ? "$teamCityBuildUrl&tab=buildLog" : missingUrlMessage }]
        | Project        : [${ teamcityProperty( 'teamcity.projectName' )}]
        | Configuration  : [${ teamcityProperty( 'teamcity.buildConfName' )}]
        | Build Number   : [${ teamcityProperty( 'build.number' )}]
        | Personal Build : [${ teamcityProperty( 'build.is.personal' )}]"""
    }


    @Ensures({ result != null })
    String serverContent()
    {
        ( project.hasProperty( 'teamcity' ) ? teamcityContent() :
          systemEnv.JENKINS_URL             ? jenkinsContent() :
          systemEnv.HUDSON_URL              ? hudsonContent() :
                                              '' )
    }


    @Ensures({ result })
    String buildContent()
    {
        buildContent0() + "\n$SEPARATOR"
    }


    @Ensures({ result })
    String buildContent0()
    {
        // noinspection GroovyPointlessBoolean
        final includeDependencies = ( ext.includeDependencies != false ) && ( ext.includeDependencies != 'false' )
        final publicIp            = publicIp()

        """
        $SEPARATOR
        | Build Info
        $SEPARATOR
        | Host          : [${ hostname() }]${ publicIp ? ' / [' + publicIp + ']' : '' }
        | Time          : [$startTimeFormatted]
        | User          : [${ systemProperties[ 'user.name' ] }]
        | ${ ext.includePaths ? 'Directory     : [' + systemProperties[ 'user.dir' ] + ']': '' }
        | Java          : [${ systemProperties[ 'java.version' ] }][${ systemProperties[ 'java.vm.vendor' ] }]${ ext.includePaths ? '[' + systemProperties[ 'java.home' ] + ']' : '' }[${ systemProperties[ 'java.vm.name' ] }]
        | OS            : [${ systemProperties[ 'os.name' ] }][${ systemProperties[ 'os.arch' ] }][${ systemProperties[ 'os.version' ] }]
        $SEPARATOR
        | Gradle Info
        $SEPARATOR
        | Version       : [${ project.gradle.gradleVersion }]
        | ${ ext.includePaths ? 'Home          : [' + project.gradle.gradleHomeDir.canonicalPath + ']' : '' }
        | ${ ext.includePaths ? 'Project dir   : [' + projectDir.canonicalPath + ']': '' }
        | ${ ext.includePaths ? 'Build file    : [' + ( project.buildFile ?: project.rootProject.buildFile ).canonicalPath + ']' : '' }
        | GRADLE_OPTS   : [${ systemEnv.GRADLE_OPTS ?: '' }]
        | Project       : [${ ext.includePaths ? project.toString() : project.toString().replaceAll( /\s+@.+/, '' )}]
        | Tasks         : ${ project.gradle.startParameter.taskNames }
        | Coordinates   : [$project.group:$project.name:$project.version]
        |${ includeDependencies ? ' Dependencies  : [' + padLines( dependenciesContent(), ' Dependencies  : [' ) + ']' : '' }""" +

        ( ext.includeProperties ?

        """
        $SEPARATOR
        | Gradle Properties
        $SEPARATOR
        |${ sort( project.properties ) }""" : '' ) +

        ( ext.includeSystem ?

        """
        $SEPARATOR
        | System Properties
        $SEPARATOR
        |${ sort( systemProperties ) }""" : '' ) +

        ( ext.includeEnv ?

        """
        $SEPARATOR
        | Environment Variables
        $SEPARATOR
        |${ sort( systemEnv ) }""" : '' )
    }


    @SuppressWarnings([ 'GroovyPointlessBoolean' ])
    @Ensures({ result })
    String dependenciesContent()
    {
        assert ( ext.includeDependencies != false ) && ( ext.includeDependencies != 'false' )

        project.plugins.apply( ProjectReportsPlugin )

        final reportTask = ( DependencyReportTask ) project.tasks[ ProjectReportsPlugin.DEPENDENCY_REPORT ]
        final renderer   = asciiReportRenderer()
        final file       = new File( buildDir(), "${ this.class.name }-dependencies.txt" )
        final line       = '-' * 80
        delete( file )

        renderer.outputFile = file
        reportTask.renderer = renderer
        reportTask.generate( project )

        assert file.file, "File [$file.canonicalPath] was not created by dependency report"
        String report = ( ext.includeDependencies instanceof List ) ?
               read( file ).split( '\n\n' ).findAll { find( it, ext.configurationNamePattern ) in (( List<String> ) ext.includeDependencies ) }.join( '\n\n' ) :
               read( file )

        report = "$line\n" + report.replaceAll( /(?m)^\s*$/, line ) // Empty lines replaced by $line
        delete( false, file ) // https://github.com/evgeny-goldin/gradle-plugins/issues/6
        report
    }


    @Ensures({ result })
    private DependencyReportRenderer asciiReportRenderer()
    {
        try
        {   // Gradle 1.2
            ( DependencyReportRenderer ) this.class.classLoader.loadClass( 'org.gradle.api.tasks.diagnostics.internal.AsciiReportRenderer' ).
            newInstance()
        }
        catch ( ClassNotFoundException ignored )
        {   // Gradle 1.3
            ( DependencyReportRenderer ) this.class.classLoader.loadClass( 'org.gradle.api.tasks.diagnostics.internal.dependencies.AsciiDependencyReportRenderer' ).
            newInstance()
        }
    }


    @Ensures({ result })
    String scmContent()
    {
        if ( ! ext.includeSCM ) { return '' }

        /**
         * Trying Git
         */

        final String gitVersion = gitExec( '--version', project.rootDir, false )
        final String gitStatus  = gitVersion.contains( 'git version' ) ? gitExec( 'status', project.rootDir, false ) : ''

        if ( gitStatus && ( ! gitStatus.with { startsWith( 'fatal:' ) || startsWith( 'error:' ) }))
        {
            /**
             * d7d53c1
             * d7d53c1f5eeba85cc02d4522990a020f02dea2b7
             * Sun, 7 Oct 2012 23:01:37 +0200
             * Evgeny Goldin
             * evgenyg@gmail.com
             * CodeNarc fix
             */
            final gitLog = gitExec( 'log -1 --format=format:%h%n%H%n%cD%n%cN%n%ce%n%B' ).readLines()*.trim()

            """
            $SEPARATOR
            | Git Info
            $SEPARATOR
            | Version        : [${ gitVersion.replace( 'git version', '' ).trim() }]
            | Repositories   : [${ padLines( gitExec( 'remote -v' ), ' Repositories   : [' ) }]
            | Branch         : [${ find( gitStatus.readLines(), '# On branch' ) }]
            | Status         : [${ padLines( gitStatus, ' Status         : [' ) }]
            | Commit         : [${ gitLog[ 0 ] }][${ gitLog[ 1 ] }]
            | Commit Date    : [${ gitLog[ 2 ] }]
            | Commit Author  : [${ gitLog[ 3 ] } <${ gitLog[ 4 ] }>]
            | Commit Message : [${ gitLog.size() > 5 ? padLines( gitLog[ 5 .. -1 ].join( '\n' ), ' Commit Message : [' ) : '' }]"""
        }
        else
        {
            """
            $SEPARATOR
            | SCM Info
            $SEPARATOR
            | Unsupported SCM system: either project is not managed by Git or command-line client is not available.
            | Tried Git:
            | ~~~~~~~~~~
            | 'git --version' returned [${ padLines( gitVersion, ' \'git --version\' returned [' ) }]
            |${ gitStatus ? ' \'git status\'    returned [' + padLines( gitStatus, ' \'git status\'    returned [' ) + ']' : '' }"""
        }
    }
}
