package org.jetbrains.kotlin.gradle.tasks

import com.google.common.io.Files
import com.google.common.io.Resources
import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments
import static org.jetbrains.jet.cli.common.ExitCode.*

/**
 * Created by Nikita.Skvortsov
 * Date: 4/28/12, 7:06 PM
 */
public class KotlinCompile extends AbstractCompile {

    private K2JVMCompiler compiler;

    public KotlinCompile() {
        compiler = new K2JVMCompiler();
    }


    @Override
    protected void compile() {
        def K2JVMCompilerArguments args = new K2JVMCompilerArguments();

        // todo better source handling (not file-by-file)
        def sources = getSource().collect {it.absolutePath}

        // todo what are the modes?
        args.mode = "stdlib"
        def classPath = getClasspath().filter {it.exists()}.asPath
        if (classPath.length() > 0) {
            args.setClasspath(classPath)
        }
        args.setSourceDirs(sources)
        args.setOutputDir(getDestinationDir().getPath())
        args.jdkAnnotations = extractJdkHeaders()
        def exitCode = compiler.exec(System.err, args);

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new GradleException("Compilation error. See log for more details");

            case INTERNAL_ERROR:
                throw new GradleException("Internal compiler error. See log for more details");
        }

    }

    private String extractJdkHeaders() {
        def kotlin_jdk_headers = "kotlin-jdk-annotations.jar"
        def jdkHeaders = Resources.getResource(kotlin_jdk_headers)

        final File jdkHeadersTempDir = Files.createTempDir();
        jdkHeadersTempDir.deleteOnExit();

        final File jdkHeadersFile = new File(jdkHeadersTempDir, kotlin_jdk_headers);
        Files.copy(Resources.newInputStreamSupplier(jdkHeaders), jdkHeadersFile);

        return jdkHeadersFile.getPath();
    }
}
