package com.github.goldin.plugins.gradle.kotlin.tasks

import static org.jetbrains.jet.cli.common.ExitCode.*

import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments


class KotlinCompile extends AbstractCompile
{

    private final K2JVMCompiler compiler = new K2JVMCompiler()


    @Override
    protected void compile()
    {
        final args            = new K2JVMCompilerArguments()
        args.noStdlib         = false
        args.noJdkAnnotations = true
        args.classpath        = classpath.filter{ File f -> f.exists() }.asPath ?: null
        args.sourceDirs       = (( Iterable<File> ) source )*.absolutePath
        args.outputDir        = destinationDir.path
        final exitCode        = compiler.exec( System.err, args )

        switch ( exitCode )
        {
            case COMPILATION_ERROR:
                throw new GradleException( 'Compilation error. See log for more details' )

            case INTERNAL_ERROR:
                throw new GradleException( 'Internal compiler error. See log for more details' )

            case OK:
            default:
                logger.info( 'Compilation successful' )
        }
    }
}
