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
        final singleSource    = ( source.files.size() == 1 )
        final args            = ( K2JVMCompilerArguments ) project.extensions.findByName( KotlinPlugin.EXTENSION_NAME )
        args.noStdlib         = true // Otherwise, Kotlin compiler attempts
        args.noJdkAnnotations = true // to locate the corresponding jar files
        args.outputDir        = ( args.outputDir == null ) ? destinationDir.canonicalPath                       : args.outputDir
        args.classpath        = ( args.classpath == null ) ? new SimpleFileCollection( classpathFiles()).asPath : args.classpath

        if ( singleSource ) { args.src        = ( args.src        == null ) ? source.singleFile.canonicalPath   : args.src }
        else                { args.sourceDirs = ( args.sourceDirs == null ) ? source.files*.canonicalPath       : args.sourceDirs }

        if ( logger.infoEnabled )
        {
            final list = { Collection c -> "* [${ c.join( ']\n* [' )}]" }
            logger.with {
                info( 'Running Kotlin compiler' )
                info( singleSource? 'src:' : 'sourceDirs:' )
                info( list( singleSource ? [ args.src ] : args.sourceDirs ))
                info( 'outputDir:' )
                info( list([ args.outputDir ]))
                info( 'classpath:' )
                info( list( args.classpath.tokenize( File.pathSeparator )))
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


    @SuppressWarnings([ 'UnnecessaryCollectCall' ])
    private Collection<File> classpathFiles()
    {
        dependsOn.findAll{ Object o          -> o instanceof AbstractCompile }.
                  collect{ AbstractCompile c -> c.destinationDir }.
                  findAll{ File f            -> f.directory } +
        classpath.filter { File f -> f.exists() }.files
    }
}
