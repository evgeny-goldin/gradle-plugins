package com.github.goldin.plugins.gradle.kotlin

import static org.jetbrains.jet.cli.common.ExitCode.*
import org.gradle.api.GradleException
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments


class KotlinCompileTask extends AbstractCompile
{
    private final K2JVMCompiler compiler = new K2JVMCompiler()


    @Override
    protected void compile()
    {
        final classpathFiles = classpath.filter { File f -> f.exists() }.files +
                               dependsOn.findAll{ Object o          -> o instanceof AbstractCompile }.
                                         findAll{ AbstractCompile c -> c.destinationDir.directory   }.
                                         collect{ AbstractCompile c -> c.destinationDir }

        final args            = ( K2JVMCompilerArguments ) project.extensions.findByName( KotlinPlugin.EXTENSION_NAME )
        args.noStdlib         = true // Otherwise, Kotlin compiler
        args.noJdkAnnotations = true // attempts to locate the corresponding jar files
        args.sourceDirs       = source.files*.canonicalPath
        args.outputDir        = destinationDir.canonicalPath
        args.classpath        = new SimpleFileCollection( classpathFiles ).asPath

        if ( logger.infoEnabled )
        {
            final list = { Collection c -> "* [${ c.join( ']\n* [' )}]" }
            logger.with {
                info( 'Running Kotlin compiler' )
                info( 'sourceDirs:' )
                info( list( args.sourceDirs ))
                info( 'outputDir:' )
                info( list([ args.outputDir ]))
                info( 'classpath:' )
                info( list( args.classpath.tokenize( System.getProperty( 'path.separator' ))))
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
