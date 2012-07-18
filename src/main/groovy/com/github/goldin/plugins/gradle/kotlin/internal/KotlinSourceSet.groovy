package com.github.goldin.plugins.gradle.kotlin.internal

import org.gradle.api.file.SourceDirectorySet


interface KotlinSourceSet
{
    SourceDirectorySet getKotlin()
    KotlinSourceSet kotlin(Closure configureClosure)
}
