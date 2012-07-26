package com.github.goldin.plugins.gradle.kotlin

import static org.jetbrains.jet.cli.common.ExitCode.*

import org.gradle.api.GradleException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.Compile
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments


class KotlinCompileTask extends AbstractCompile
{
    private final K2JVMCompiler compiler = new K2JVMCompiler()


    @Override
    protected void compile()
    {
        final extension       = ( KotlinCompileTaskExtension ) project[ KotlinCompilePlugin.COMPILE_EXTENSION_NAME ]
        final args            = new K2JVMCompilerArguments()
        args.noStdlib         = true
        args.noJdkAnnotations = true
        args.classpath        = classpath.filter{ File f -> f.exists() }.asPath ?: null
        args.sourceDirs       = source.files*.canonicalPath
        args.outputDir        = destinationDir.canonicalPath

        if ( extension.dependsOnJava )
        {
            final javaDestinationDir = (( Compile ) project.tasks.findByName( JavaPlugin.COMPILE_JAVA_TASK_NAME )).destinationDir
            if ( javaDestinationDir?.directory )
            {
                args.classpath = args.classpath ? "${ args.classpath };${ javaDestinationDir.canonicalPath }" :
                                 javaDestinationDir.canonicalPath
            }
        }

        final exitCode = compiler.exec( System.err, args )

        switch ( exitCode )
        {
            case COMPILATION_ERROR:
                throw new GradleException( 'Compilation error. See log for more details' )

            case INTERNAL_ERROR:
                throw new GradleException( 'Internal compiler error. See log for more details' )

            default:
                logger.info( 'Compilation successful' )
        }
    }
}
