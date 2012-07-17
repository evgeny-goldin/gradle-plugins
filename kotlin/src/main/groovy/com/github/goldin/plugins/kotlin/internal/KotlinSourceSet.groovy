package com.github.goldin.plugins.kotlin.internal

import org.gradle.api.file.SourceDirectorySet


interface KotlinSourceSet
{
    SourceDirectorySet getKotlin()
    KotlinSourceSet kotlin(Closure configureClosure)
}
