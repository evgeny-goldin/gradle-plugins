package com.github.goldin.plugins.gradle.kotlin.tasks

import static org.jetbrains.jet.cli.common.ExitCode.*

import com.google.common.io.Files
import com.google.common.io.Resources
import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments


class KotlinCompile extends AbstractCompile {

    private final K2JVMCompiler compiler = new K2JVMCompiler()


    @Override
    protected void compile() {
        final args = new K2JVMCompilerArguments()

        // todo better source handling (not file-by-file)
        final sources = source*.absolutePath

        // todo what are the modes?
        args.mode = 'stdlib'
        final classPath = classpath.filter {it.exists()}.asPath
        if (classPath.length() > 0) {
            args.setClasspath(classPath)
        }
        args.setSourceDirs( sources )
        args.setOutputDir( destinationDir.path )
        args.jdkAnnotations = extractJdkHeaders()
        final exitCode      = compiler.exec(System.err, args)

        switch (exitCode) {
            case COMPILATION_ERROR:
                throw new GradleException('Compilation error. See log for more details')
            case INTERNAL_ERROR:
                throw new GradleException('Internal compiler error. See log for more details')
        }
    }


    private String extractJdkHeaders() {
        final kotlinJdkHeaders = 'kotlin-jdk-annotations.jar'
        final jdkHeaders       = Resources.getResource(kotlinJdkHeaders)

        final File jdkHeadersTempDir = Files.createTempDir()
        jdkHeadersTempDir.deleteOnExit()

        final File jdkHeadersFile = new File(jdkHeadersTempDir, kotlinJdkHeaders)
        Files.copy(Resources.newInputStreamSupplier(jdkHeaders), jdkHeadersFile)

        jdkHeadersFile.path
    }
}
