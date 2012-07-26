package com.github.goldin.plugins.gradle.kotlin


/**
 * {@link KotlinCompileTask} extension.
 */
class KotlinCompileTaskExtension
{
    /**
     * Whether Kotlin compilation depends on Java compilation.
     */
    boolean dependsOnJava = false;
}
