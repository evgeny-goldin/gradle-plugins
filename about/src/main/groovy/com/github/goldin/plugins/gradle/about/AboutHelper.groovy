package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.BaseTask
import com.github.goldin.plugins.gradle.common.helpers.BaseHelper
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.Project


/**
 * {@link AboutTask} helper class.
 */
class AboutHelper extends BaseHelper<AboutExtension>
{
    @SuppressWarnings([ 'GroovyUntypedAccess' ])
    @Requires({ project && task && ext })
    @Ensures ({ this.project && this.task && this.ext })
    AboutHelper ( Project project, BaseTask task, AboutExtension ext ){ super( project, task, ext )}


    private static final String       SEPARATOR = '|==============================================================================='
    private final Map<String, String> env        = System.getenv().asImmutable()
    private final Map<String, String> properties = ( Map<String , String> ) System.properties.asImmutable()


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
        | Server         : [${ env[ 'JENKINS_URL' ] }]
        | Job            : [${ env[ 'JENKINS_URL' ] }job/${ env[ 'JOB_NAME' ] }/${ env[ 'BUILD_NUMBER' ]}/]
        | Log            : [${ env[ 'JENKINS_URL' ] }job/${ env[ 'JOB_NAME' ] }/${ env[ 'BUILD_NUMBER' ]}/console]"""
    }


    @Ensures({ result })
    String hudsonContent()
    {
        // http://weblogs.java.net/blog/johnsmart/archive/2008/03/using_hudson_en.html

        """
        $SEPARATOR
        | Hudson Info
        $SEPARATOR
        | Server         : [${ env[ 'HUDSON_URL' ] }]
        | Job            : [${ env[ 'HUDSON_URL' ] }job/${ env[ 'JOB_NAME' ] }/${ env[ 'BUILD_NUMBER' ]}/]
        | Log            : [${ env[ 'HUDSON_URL' ] }job/${ env[ 'JOB_NAME' ] }/${ env[ 'BUILD_NUMBER' ]}/console]"""
    }


    @Ensures({ result })
    String teamcityContent()
    {
        // http://confluence.jetbrains.net/display/TCD7/Predefined+Build+Parameters

        final  teamcityProperties = project.properties[ 'teamcity' ]
        assert teamcityProperties

        final urlMessage     = 'Define \'TEAMCITY_URL\' environment variable'
        final buildId        = teamcityProperties[ 'teamcity.build.id' ]
        final teamCityUrl    = ( env[ 'TEAMCITY_URL' ]?.replaceAll( /(?<!\\|\/)(\\|\/)*$/, '/' )       ?: '' )
        final buildUrl       = ( teamCityUrl && buildId ? "${teamCityUrl}viewLog.html?buildId=$buildId" : '' )
        final logUrl         = ( buildUrl               ? "$buildUrl&tab=buildLog"                      : '' )

        if ( teamCityUrl ) { assert teamCityUrl.endsWith( '/' )}

        """
        $SEPARATOR
        | TeamCity Info
        $SEPARATOR
        | Version        : [${ teamcityProperties[ 'teamcity.version' ]       ?: '' }]
        | Server         : [${ teamCityUrl ?: urlMessage }]
        | Job            : [${ buildUrl    ?: urlMessage }]
        | Log            : [${ logUrl      ?: urlMessage }]
        | Project        : [${ teamcityProperties[ 'teamcity.projectName' ]   ?: '' }]
        | Configuration  : [${ teamcityProperties[ 'teamcity.buildConfName' ] ?: '' }]
        | Build Number   : [${ teamcityProperties[ 'build.number' ]           ?: '' }]
        | Personal Build : [${ teamcityProperties[ 'build.is.personal' ]      ?: 'false' }]"""
    }


    @Ensures({ result != null })
    String serverContent()
    {
        ( project.hasProperty( 'teamcity' ) ? teamcityContent() :
          env[ 'JENKINS_URL' ]              ? jenkinsContent () :
          env[ 'HUDSON_URL'  ]              ? hudsonContent  () :
                                              '' )
    }


    @Ensures({ result })
    String buildContent ()
    {
        buildContent0() + "\n$SEPARATOR"
    }


    @Ensures({ result })
    String buildContent0 ()
    {
        // noinspection GroovyPointlessBoolean
        final includeDependencies = ( ext.includeDependencies != false ) && ( ext.includeDependencies != 'false' )

        """
        $SEPARATOR
        | Build Info
        $SEPARATOR
        | Host          : [${ hostname() }]${ publicIp().with { delegate ? '/[' + delegate + ']' : '' }}
        | Time          : [$startTimeFormatted]
        | User          : [${ properties[ 'user.name' ] }]
        | ${ ext.includePaths ? 'Directory     : [' + properties[ 'user.dir' ] + ']': '' }
        | Java          : [${ properties[ 'java.version' ] }][${ properties[ 'java.vm.vendor' ] }]${ ext.includePaths ? '[' + properties[ 'java.home' ] + ']' : '' }[${ properties[ 'java.vm.name' ] }]
        | OS            : [${ properties[ 'os.name' ] }][${ properties[ 'os.arch' ] }][${ properties[ 'os.version' ] }]
        $SEPARATOR
        | Gradle Info
        $SEPARATOR
        | Version       : [${ project.gradle.gradleVersion }]
        | ${ ext.includePaths ? 'Home          : [' + project.gradle.gradleHomeDir.canonicalPath + ']' : '' }
        | ${ ext.includePaths ? 'Project dir   : [' + projectDir.canonicalPath + ']': '' }
        | ${ ext.includePaths ? 'Build file    : [' + ( project.buildFile ?: project.rootProject.buildFile ).canonicalPath + ']' : '' }
        | GRADLE_OPTS   : [${ env[ 'GRADLE_OPTS' ] ?: '' }]
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
        |${ sort( properties ) }""" : '' ) +

        ( ext.includeEnv ?

        """
        $SEPARATOR
        | Environment Variables
        $SEPARATOR
        |${ sort( env ) }""" : '' )
    }


    @Ensures({ result })
    String dependenciesContent ()
    {
        assert ( ext.includeDependencies != false ) && ( ext.includeDependencies != 'false' )

        project.plugins.apply( ProjectReportsPlugin )

        final reportTask = ( DependencyReportTask ) project.tasks[ ProjectReportsPlugin.DEPENDENCY_REPORT ]
        final renderer   = asciiReportRenderer()
        final file       = new File( project.buildDir, "${ this.class.name }-dependencies.txt" )
        final line       = '-' * 80
        delete( file )

        renderer.outputFile = file
        reportTask.renderer = renderer
        reportTask.generate( project )

        assert file.file, "File [$file.canonicalPath] was not created by dependency report"
        final String report = ( ext.includeDependencies instanceof List ) ?
            file.getText( 'UTF-8' ).split( '\n\n' ).findAll { find( it, ext.configurationNamePattern ) in ext.includeDependencies }.join( '\n\n' ) :
            file.getText( 'UTF-8' )

        report = "$line\n" + report.replaceAll( /(?m)^\s*$/, line ) // Empty lines replaced by $line
        delete( false, file ) // https://github.com/evgeny-goldin/gradle-plugins/issues/6
        report
    }


    @Ensures({ result })
    private DependencyReportRenderer asciiReportRenderer ()
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
