package org.jetbrains.kotlin.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.SourceTask;
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments;

import static org.jetbrains.jet.cli.common.ExitCode.COMPILATION_ERROR;
import static org.jetbrains.jet.cli.common.ExitCode.INTERNAL_ERROR
import org.gradle.api.tasks.TaskAction
import org.jetbrains.jet.cli.common.CompilerArguments
import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocConfig
import org.jetbrains.kotlin.doc.KDocCompiler;

/**
 * Created by Nikita.Skvortsov
 * Date: 7/6/12, 8:32 PM
 */
public class KDoc extends SourceTask {

    /**
     * Returns the directory to use to output the API docs
     */
    public destinationDir

    /**
     * Returns the name of the documentation set
     */
    public title

    /**
     * Returns the version name of the documentation set
     */
    public version

    /**
     * Returns a map of the package prefix to the HTML URL for the root of the apidoc using javadoc/kdoc style
     * directory layouts so that this API doc report can link to external packages
     */
    public Map<String, String> packagePrefixToUrls

    /**
     * Returns a Set of the package name prefixes to ignore from the KDoc report
     */
    public Set<String> ignorePackages

    /**
     * Returns true if a warning should be generated if there are no comments
     * on documented function or property
     */
    public Boolean warnNoComments

    /**
     * Returns the HTTP URL of the root directory of source code that we should link to
     */
    public String sourceRootHref

    /**
     * The root project directory used to deduce relative file names when linking to source code
     */
    public String projectRootDir

    /**
     * A map of package name to html or markdown files used to describe the package. If none is
     * speciied we will look for a package.html or package.md file in the source tree
     */
    public Map<String,String> packageDescriptionFiles

    /**
     * A map of package name to summary text used in the package overviews
     */
    public Map<String,String> packageSummaryText


    @TaskAction
    protected void generateDocs() {
        def KDocArguments args = new KDocArguments();
        def KDocConfig cfg = args.docConfig

        destinationDir ? cfg.docOutputDir = destinationDir :'';
        title ? cfg.title = title :'';
        version ? cfg.version = version :'';
        sourceRootHref ? cfg.sourceRootHref = sourceRootHref :'';
        projectRootDir ? cfg.projectRootDir = projectRootDir :'';
        if (warnNoComments != null) {
            cfg.warnNoComments = warnNoComments;
        }

        packagePrefixToUrls ? cfg.packagePrefixToUrls.putAll(packagePrefixToUrls) :'';
        ignorePackages ? cfg.ignorePackages.addAll(ignorePackages):'';
        packageDescriptionFiles ? cfg.packageDescriptionFiles.putAll(packageDescriptionFiles) :'';
        packageSummaryText ? cfg.packageSummaryText.putAll(packageSummaryText) :'';

        def KDocCompiler compiler = new KDocCompiler();
        def exitCode = compiler.exec(System.err, args);

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new GradleException("Failed to generate kdoc. See log for more details");

            case INTERNAL_ERROR:
                throw new GradleException("Internal generation error. See log for more details");
        }

    }
}
