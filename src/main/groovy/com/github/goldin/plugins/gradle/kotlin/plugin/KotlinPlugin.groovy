package com.github.goldin.plugins.gradle.kotlin.plugin


import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSetImpl
import org.jetbrains.kotlin.gradle.tasks.KDoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import java.util.concurrent.Callable


public class KotlinPlugin implements Plugin<Project> {

    public static final String KDOC_TASK_NAME = "kdoc"

    @Override
    void apply(Project project) {
        def javaBasePlugin = project.plugins.apply(JavaBasePlugin.class)
        def javaPluginConvention = project.convention.getPlugin(JavaPluginConvention.class)
        project.plugins.apply(JavaPlugin.class)
        configureSourceSetDefaults(project, javaBasePlugin, javaPluginConvention);
        configureKDoc(project, javaPluginConvention);
    }

    private void configureSourceSetDefaults(Project project,
                                            JavaBasePlugin javaBasePlugin,
                                            JavaPluginConvention javaPluginConvention) {
        javaPluginConvention.sourceSets.all { SourceSet sourceSet ->
            sourceSet.convention.plugins.kotlin = new KotlinSourceSetImpl(sourceSet.displayName, project.fileResolver)
            sourceSet.kotlin.srcDir { project.file("src/$sourceSet.name/kotlin") }
            sourceSet.allJava.source(sourceSet.kotlin)
            sourceSet.allSource.source(sourceSet.kotlin)

            sourceSet.resources.filter.exclude { FileTreeElement elem -> sourceSet.kotlin.contains(elem.file) }

            String kotlinTaskName = sourceSet.getCompileTaskName("kotlin")
            KotlinCompile kotlinTask = project.tasks.add(kotlinTaskName , KotlinCompile.class)
            javaBasePlugin.configureForSourceSet(sourceSet, kotlinTask)
            kotlinTask.description = "Compiles the $sourceSet.kotlin."
            kotlinTask.source = sourceSet.kotlin
            def javaTask = project.tasks.findByName(sourceSet.compileJavaTaskName)
            javaTask.dependsOn kotlinTaskName
        }
    }

    private void configureKDoc(Project project, JavaPluginConvention javaPluginConvention) {
        SourceSet mainSourceSet = javaPluginConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        KDoc kdoc = project.getTasks().add(KDOC_TASK_NAME, KDoc.class);
        kdoc.description = "Generates KDoc API documentation for the main source code.";
        kdoc.group = JavaBasePlugin.DOCUMENTATION_GROUP;
        kdoc.source = mainSourceSet.kotlin

        project.getTasks().withType(KDoc.class, new Action<KDoc>() {
            public void execute(KDoc param) {
                param.getConventionMapping().map("destinationDir", new Callable<Object>() {
                    public Object call() throws Exception {
                        return new File(javaPluginConvention.getDocsDir(), "kdoc");
                    }
                });
            }
        });
    }
}
