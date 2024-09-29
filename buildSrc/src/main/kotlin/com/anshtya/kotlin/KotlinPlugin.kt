package com.anshtya.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile
import java.net.URLClassLoader

open class KotlinCompileTask : DefaultTask() {
    @get:InputFiles
    val kotlinSources = project.fileTree(
        project.projectDir.resolve("src")
    ).matching { include("**/*.kt") }

    @get:OutputDirectory
    val outputDir = project.layout.buildDirectory.dir("classes")

    @get:Classpath
    val dependencies = project.files()

    @get:Input
    val moduleName = project.name

    @TaskAction
    fun compile() {
        val outputDirFile = outputDir.get().asFile
        with(outputDirFile) {
            deleteRecursively()
            mkdirs()
        }

        val arg = K2JVMCompilerArguments()
        arg.apply {
            noStdlib = true
            moduleName = this@KotlinCompileTask.moduleName
            classpathAsList = dependencies.map { file -> file.absoluteFile }
            destinationAsFile = outputDirFile
            freeArgs = kotlinSources.map { it.absolutePath }
        }

        val exitCode = K2JVMCompiler().exec(
            errStream = System.err,
            messageRenderer = MessageRenderer.GRADLE_STYLE,
            args = arg.toArgumentStrings().toTypedArray()
        )

        if (exitCode != ExitCode.OK) {
            error("Did not compile: $exitCode")
        }
    }
}

open class KotlinRunTask : DefaultTask() {
    @get:Classpath
    val classpath = project.files()

    @get:Input
    val mainClass = project.objects.property<String>()

    @TaskAction
    fun run() {
        val classLoader = URLClassLoader.newInstance(
            classpath
                .map { it.toPath().toUri().toURL() }
                .toTypedArray()
        )

        val mainClass = classLoader.loadClass(mainClass.get())
        val mainMethod = mainClass?.getMethod("main")
        mainMethod?.invoke(null)
    }
}

class KotlinPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val compileKotlinTask = target.tasks.register<KotlinCompileTask>("compileKotlin") {}
            val runKotlinTask = target.tasks.register<KotlinRunTask>("runKotlin") {}

            configurations.create("apiElements") {
                isCanBeResolved = false
                isCanBeConsumed = true

                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage.JAVA_API))
                    attribute(Category.CATEGORY_ATTRIBUTE, target.objects.named(Category.LIBRARY))
                }

                outgoing.artifact(compileKotlinTask)
            }

            val implementationConfiguration = configurations.create("implementation") {
                isCanBeResolved = true
                isCanBeConsumed = false

                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, target.objects.named(Usage.JAVA_API))
                    attribute(Category.CATEGORY_ATTRIBUTE, target.objects.named(Category.LIBRARY))
                }
            }

            compileKotlinTask.configure {
                dependencies.from(implementationConfiguration)
            }

            runKotlinTask.configure {
                classpath.from(implementationConfiguration)
                classpath.from(project.files(compileKotlinTask.map { it.outputDir }))
            }
        }
    }
}