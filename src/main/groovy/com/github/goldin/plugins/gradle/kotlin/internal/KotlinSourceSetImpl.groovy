package com.github.goldin.plugins.gradle.kotlin.internal

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.util.ConfigureUtil;
import org.jetbrains.annotations.NotNull;


public class KotlinSourceSetImpl implements KotlinSourceSet
{
    private final SourceDirectorySet kotlin

    public KotlinSourceSetImpl(@NotNull final String displayName, @NotNull final FileResolver resolver) {
        kotlin = new DefaultSourceDirectorySet(displayName + " Kotlin source", resolver)
        kotlin.getFilter().include("**/*.java", "**/*.kt")
    }

    @Override
    public SourceDirectorySet getKotlin() {
        kotlin
    }

    @Override
    public KotlinSourceSet kotlin(Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, getKotlin())
        this
    }
}
