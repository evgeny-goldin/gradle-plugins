package com.github.goldin.plugins.gradle.kotlin.tasks

import static org.jetbrains.jet.cli.common.ExitCode.*

import org.gradle.api.GradleException
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocCompiler


class KDoc extends SourceTask {

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

        destinationDir ? cfg.docOutputDir  = destinationDir  : ''
        title          ? cfg.title          = title          : ''
        version        ? cfg.version        = version        : ''
        sourceRootHref ? cfg.sourceRootHref = sourceRootHref : ''
        projectRootDir ? cfg.projectRootDir = projectRootDir : ''

        if (warnNoComments != null)
        {
            cfg.warnNoComments = warnNoComments
        }

        packagePrefixToUrls     ? cfg.packagePrefixToUrls.putAll(packagePrefixToUrls)         : ''
        ignorePackages          ? cfg.ignorePackages.addAll(ignorePackages)                   : ''
        packageDescriptionFiles ? cfg.packageDescriptionFiles.putAll(packageDescriptionFiles) : ''
        packageSummaryText      ? cfg.packageSummaryText.putAll(packageSummaryText)           : ''

        final compiler = new KDocCompiler()
        final exitCode = compiler.exec(System.err, args)

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new GradleException('Failed to generate kdoc. See log for more details')
            case INTERNAL_ERROR:
                throw new GradleException('Internal generation error. See log for more details')
        }
    }
}
