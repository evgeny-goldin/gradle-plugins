package com.github.goldin.plugins.gradle.kotlin

import static org.jetbrains.jet.cli.common.ExitCode.*

import org.gradle.api.GradleException
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.Compile
import org.jetbrains.jet.cli.jvm.K2JVMCompiler
import org.jetbrains.jet.cli.jvm.K2JVMCompilerArguments


class KotlinTask extends AbstractCompile
{
    private final K2JVMCompiler compiler = new K2JVMCompiler()
    private final static String DELIM    = System.getProperty( 'path.separator' )


    @Override
    protected void compile()
    {
        final args            = new K2JVMCompilerArguments()
        args.noStdlib         = true
        args.noJdkAnnotations = true
        args.classpath        = classpath.filter{ File f -> f.exists() }.asPath ?: null
        args.sourceDirs       = source.files*.canonicalPath
        args.outputDir        = destinationDir.canonicalPath
        final list            = { Collection c -> "* [${ c.join( ']\n* [' )}]"}

        for ( compileTask in dependsOn.findAll{ it instanceof AbstractCompile } )
        {
            final destinationDir = (( Compile ) compileTask ).destinationDir
            if ( destinationDir.directory )
            {
                destinationDir.canonicalPath.with {
                    args.classpath = args.classpath ? "${ args.classpath }${ DELIM }${ delegate }" : delegate
                }
            }
        }

        if ( logger.infoEnabled )
        {
            logger.info( "Running Kotlin compiler" )
            logger.info( "sourceDirs:" )
            logger.info( list( args.sourceDirs ))
            logger.info( "outputDir:" )
            logger.info( list([ args.outputDir ]))
            logger.info( "classpath:" )
            logger.info( list( args.classpath.split( System.getProperty( 'path.separator' )).toList()))
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
