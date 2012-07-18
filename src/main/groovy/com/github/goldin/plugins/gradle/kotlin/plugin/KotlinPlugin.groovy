package com.github.goldin.plugins.gradle.kotlin.plugin

import com.github.goldin.plugins.gradle.kotlin.internal.KotlinSourceSetImpl
import com.github.goldin.plugins.gradle.kotlin.tasks.KDoc
import com.github.goldin.plugins.gradle.kotlin.tasks.KotlinCompile
import org.gcontracts.annotations.Requires
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

import java.util.concurrent.Callable


class KotlinPlugin implements Plugin<Project>
{

    public static final String KDOC_TASK_NAME = 'kdoc'

    @Requires({ project })
    @Override
    void apply(Project project)
    {
        final javaBasePlugin       = project.plugins.apply(JavaBasePlugin)
        final javaPluginConvention = project.convention.getPlugin(JavaPluginConvention)
        project.plugins.apply(JavaPlugin)
        configureSourceSetDefaults(project, javaBasePlugin, javaPluginConvention);
        configureKDoc(project, javaPluginConvention);
    }


    private void configureSourceSetDefaults( Project              project,
                                             JavaBasePlugin       javaBasePlugin,
                                             JavaPluginConvention javaPluginConvention )
    {
        javaPluginConvention.sourceSets.all { SourceSet sourceSet ->
            sourceSet.convention.plugins.kotlin = new KotlinSourceSetImpl(sourceSet.displayName, project.fileResolver)
            sourceSet.kotlin.srcDir { project.file("src/$sourceSet.name/kotlin") }
            sourceSet.allJava.source(sourceSet.kotlin)
            sourceSet.allSource.source(sourceSet.kotlin)

            sourceSet.resources.filter.exclude { FileTreeElement elem -> sourceSet.kotlin.contains(elem.file) }

            String kotlinTaskName = sourceSet.getCompileTaskName('kotlin')
            KotlinCompile kotlinTask = project.tasks.add(kotlinTaskName , org.jetbrains.kotlin.gradle.tasks.KotlinCompile.class)
            javaBasePlugin.configureForSourceSet(sourceSet, kotlinTask)
            kotlinTask.description = "Compiles the $sourceSet.kotlin."
            kotlinTask.source = sourceSet.kotlin
            def javaTask = project.tasks.findByName(sourceSet.compileJavaTaskName)
            javaTask.dependsOn kotlinTaskName
        }
    }

    private void configureKDoc(Project project, JavaPluginConvention javaPluginConvention)
    {
        SourceSet mainSourceSet = javaPluginConvention.sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME )
        KDoc kdoc               = project.tasks.add( KDOC_TASK_NAME, KDoc )
        kdoc.description        = 'Generates KDoc API documentation for the main source code.'
        kdoc.group              = JavaBasePlugin.DOCUMENTATION_GROUP;
        kdoc.source             = mainSourceSet.kotlin

        project.tasks.withType( KDoc, new Action<KDoc>() {
            void execute(KDoc param) {
                param.conventionMapping.map( 'destinationDir', new Callable<Object>() {
                    Object call() throws Exception {
                        new File( javaPluginConvention.docsDir, 'kdoc' )
                    }
                })
            }
        })
    }
}
