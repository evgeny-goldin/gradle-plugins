package com.github.goldin.plugins.gradle.kotlin

import static org.jetbrains.jet.cli.common.ExitCode.*

import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocCompiler


class KDocTask extends SourceTask
{
    /**
     * Returns the directory to use to output the API docs
     */
    File destinationDir

    /**
     * Returns the name of the documentation set
     */
    String title

    /**
     * Returns the version name of the documentation set
     */
    String version

    /**
     * Returns a map of the package prefix to the HTML URL for the root of the apidoc using javadoc/kdoc style
     * directory layouts so that this API doc report can link to external packages
     */
    Map<String, String> packagePrefixToUrls

    /**
     * Returns a Set of the package name prefixes to ignore from the KDoc report
     */
    Set<String> ignorePackages

    /**
     * Returns true if a warning should be generated if there are no comments
     * on documented function or property
     */
    Boolean warnNoComments

    /**
     * Returns the HTTP URL of the root directory of source code that we should link to
     */
    String sourceRootHref

    /**
     * The root project directory used to deduce relative file names when linking to source code
     */
    String projectRootDir

    /**
     * A map of package name to html or markdown files used to describe the package. If none is
     * speciied we will look for a package.html or package.md file in the source tree
     */
    Map<String,String> packageDescriptionFiles

    /**
     * A map of package name to summary text used in the package overviews
     */
    Map<String,String> packageSummaryText


    @TaskAction
    protected void generateDocs()
    {
        final args = new KDocArguments()
        final cfg  = args.docConfig

        if ( destinationDir          ){ cfg.docOutputDir   = destinationDir }
        if ( title                   ){ cfg.title          = title          }
        if ( version                 ){ cfg.version        = version        }
        if ( sourceRootHref          ){ cfg.sourceRootHref = sourceRootHref }
        if ( projectRootDir          ){ cfg.projectRootDir = projectRootDir }
        if ( warnNoComments != null  ){ cfg.warnNoComments = warnNoComments }
        if ( packagePrefixToUrls     ){ cfg.packagePrefixToUrls.putAll( packagePrefixToUrls )}
        if ( ignorePackages          ){ cfg.ignorePackages.addAll( ignorePackages )}
        if ( packageDescriptionFiles ){ cfg.packageDescriptionFiles.putAll( packageDescriptionFiles )}
        if ( packageSummaryText      ){ cfg.packageSummaryText.putAll( packageSummaryText )}

        final compiler = new KDocCompiler()
        final exitCode = compiler.exec( System.err, args )

        switch ( exitCode )
        {
            case COMPILATION_ERROR:
                throw new GradleException( 'Failed to generate KDoc. See log for more details' )

            case INTERNAL_ERROR:
                throw new GradleException( 'Internal KDoc generation error. See log for more details' )

            case OK:
            default:
                logger.info( 'KDoc generation successful' )
        }
    }
}
