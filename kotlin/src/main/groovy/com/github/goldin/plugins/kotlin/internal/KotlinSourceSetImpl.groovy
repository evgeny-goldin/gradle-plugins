package com.github.goldin.plugins.kotlin.internal
import org.gcontracts.annotations.Requires
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.util.ConfigureUtil


class KotlinSourceSetImpl implements KotlinSourceSet
{
    private final SourceDirectorySet kotlin

    @Requires({ displayName && resolver })
    KotlinSourceSetImpl( String displayName, FileResolver resolver )
    {
        kotlin = new DefaultSourceDirectorySet( displayName + ' Kotlin source', resolver )
        kotlin.filter.include( '**/*.java', '**/*.kt' )
    }


    @Override
    SourceDirectorySet getKotlin() { kotlin }


    @Override
    @SuppressWarnings([ 'ConfusingMethodName' ])
    KotlinSourceSet kotlin( Closure configureClosure ) {
        ConfigureUtil.configure( configureClosure, getKotlin())
        this
    }
}
