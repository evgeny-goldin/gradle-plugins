package com.github.goldin.plugins.gradle.kotlin.internal

import org.gradle.api.file.SourceDirectorySet;


public interface KotlinSourceSet
{
    SourceDirectorySet getKotlin()
    KotlinSourceSet kotlin(Closure configureClosure)
}
