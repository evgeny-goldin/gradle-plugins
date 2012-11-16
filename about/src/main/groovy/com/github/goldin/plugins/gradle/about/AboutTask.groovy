package com.github.goldin.plugins.gradle.about

import com.github.goldin.plugins.gradle.common.BaseTask
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.gradle.api.GradleException
import org.gradle.api.tasks.bundling.Zip


/**
 * {@link AboutPlugin} task
 */
class AboutTask extends BaseTask<AboutExtension>
{
    @Override
    AboutExtension verifyExtension ( AboutExtension ext, String description ) { ext }


    void taskAction()
    {
        final archives = archivesToUpdate()
        if ( ! archives ) { return }

        final aboutFile = createAboutFile()

        updateArchives( aboutFile, archives )
        assert project.delete( aboutFile )
    }


    /**
     * Retrieves zip archives to be updated by the plugin.
     * @return zip archives to be updated by the plugin
     */
    @Ensures({ result != null })
    private Collection<File> archivesToUpdate ()
    {
        final ext          = ext()
        List<Zip> zipTasks = ext.zipTasks ? ( ext.tasks ?: project.tasks.withType( Zip ).toList()) : []
        final notExecuted  = zipTasks.findAll { ! it.state.executed }

        if ( notExecuted )
        {
            throw new GradleException( 'The following tasks have not been executed yet: ' +
                                       notExecuted*.toString().collect{ it.replace( 'task ', '' ) }.join( ', ' ))
        }

        final zipTasksFiles = ext.zipTasks ? zipTasks*.archivePath.findAll { it.file } : []
        final split         = ext.patterns ? { String s -> s ? s.split( ',' )*.trim().grep() : null } : null
        final patternsFiles = ext.patterns ? files( ext.directory ?: project.buildDir, split( ext.include ), split( ext.exclude )) : []
        final files         = ( zipTasksFiles + patternsFiles ).toSet()

        assert ( files || ( ! ext.failIfNotFound )), "Failed to find files to update"
        assert files.every { it.file }
        files
    }


    /**
     * Creates about file
     * @return about file generated, should be deleted when task has finished!
     */
    @Ensures({ result.file })
    private File createAboutFile ()
    {
        final ext       = ext()
        final aboutFile = new File( temporaryDir, ext.fileName ?: 'about.txt' )
        final helper    = new AboutTaskHelper( this )
        final version   = readVersion()

        logger.info( "Generating \"about\" in [$aboutFile.canonicalPath] .." )

        aboutFile.write(( " Generated by http://evgeny-goldin.com/wiki/Gradle-about-plugin, version [$version]\n" +
                         helper.serverContent() + helper.scmContent() + helper.buildContent()).
                       stripMargin().readLines()*.replaceAll( /\s+$/, '' ).grep().
                       join(( 'windows' == ext.endOfLine ) ? '\r\n' : '\n' ))

        logger.info( "Generated  \"about\" in [$aboutFile.canonicalPath]" )
        aboutFile
    }


    @Ensures({ result })
    private String readVersion()
    {
        final  propertiesFile = 'META-INF/gradle-plugins/about.properties'
        final  inputStream    = this.class.classLoader.getResourceAsStream( propertiesFile )
        assert inputStream, "Unable to load [$propertiesFile]"

        final properties = new Properties()
        properties.load( inputStream )
        properties[ 'version' ]
    }


    /**
     * Updates all archives with the "about" data.
     *
     * @param aboutFile about file
     * @param archives zip archives to update
     */
    @Requires({ aboutFile.file && archives })
    private void updateArchives( File aboutFile, Collection<File> archives )
    {
        final ext    = ext()
        final prefix = (( ext.prefix == '/' ) ? '' : ext.prefix )

        for ( archive in archives )
        {
            final aboutPath = "$archive.canonicalPath!$prefix${ ( prefix && ( ! prefix.endsWith( '/' ))) ? '/' : '' }${ aboutFile.name }"
            logger.info( "Adding \"about\" to [$aboutPath] .." )
            ant.zip( destfile : archive, update : true ){ zipfileset( file : aboutFile, prefix : prefix )}
            logger.info( "Added  \"about\" to [$aboutPath]" )
        }
    }
}
