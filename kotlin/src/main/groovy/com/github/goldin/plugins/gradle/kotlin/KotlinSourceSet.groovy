package com.github.goldin.plugins.gradle.kotlin

import org.gradle.api.file.SourceDirectorySet


interface KotlinSourceSet
{
    SourceDirectorySet getKotlin()
    KotlinSourceSet kotlin(Closure configureClosure)
}
