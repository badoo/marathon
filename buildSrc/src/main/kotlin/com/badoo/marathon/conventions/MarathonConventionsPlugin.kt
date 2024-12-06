package com.badoo.marathon.conventions

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class MarathonConventionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val versionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

        project.group = "com.github.badoo.marathon"
        project.version = getMarathonVersion(project.providers, versionCatalog)

        project.plugins.withId("java") {
            project.plugins.apply("maven-publish")
            project.configureJava(versionCatalog)
            project.configureTesting(versionCatalog)
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.plugins.apply("io.gitlab.arturbosch.detekt")
            project.configureKotlin()
        }

        project.plugins.withId("io.gitlab.arturbosch.detekt") {
            project.configureDetekt()
        }

        project.plugins.withId("maven-publish") {
            project.configurePublishing()
        }

        // Disable Gradle module metadata to avoid publishing BOM dependencies
        project.tasks.withType<GenerateModuleMetadata>().configureEach {
            enabled = false
        }
    }

    private fun Project.configureJava(versionCatalog: VersionCatalog) {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JVM_TARGET)
            }
            withJavadocJar()
            withSourcesJar()
        }

        dependencies {
            add("implementation", platform(versionCatalog.findLibrary("kotlin-bom").get()))
            add("implementation", platform(versionCatalog.findLibrary("kotlinx-coroutines-bom").get()))
            add("implementation", platform(versionCatalog.findLibrary("ktor-bom").get()))
        }
    }

    private fun Project.configureKotlin() {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain {
                languageVersion.set(JVM_TARGET)
            }
            compilerOptions {
                freeCompilerArgs.addAll(
                    "-Xjvm-default=all",
                    "-Xconsistent-data-class-copy-visibility"
                )
                optIn.addAll(
                    "kotlin.RequiresOptIn"
                )
            }
        }
    }

    private fun Project.configureDetekt() {
        configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.from(rootDir.resolve("detekt.yml"))
        }
    }

    private fun Project.configureTesting(versionCatalog: VersionCatalog) {
        dependencies {
            add("implementation", platform(versionCatalog.findLibrary("junit-bom").get()))
            add("testImplementation", versionCatalog.findLibrary("kluent").get())
            add("testImplementation", versionCatalog.findLibrary("mockito-kotlin").get())
            add("testImplementation", versionCatalog.findLibrary("junit-jupiter-api").get())
            add("testImplementation", versionCatalog.findLibrary("spek-api").get())
            add("testRuntimeOnly", versionCatalog.findLibrary("junit-jupiter-engine").get())
            add("testRuntimeOnly", versionCatalog.findLibrary("junit-platform-launcher").get())
            add("testRuntimeOnly", versionCatalog.findLibrary("spek-engine").get())
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform {
                includeEngines("spek", "junit-jupiter")
            }
        }
    }

    private fun Project.configurePublishing() {
        configure<PublishingExtension> {
            publications {
                if (!plugins.hasPlugin("java-gradle-plugin")) {
                    create<MavenPublication>("maven") {
                        from(components["java"])
                        customizePom()
                    }
                }
                withType<MavenPublication>().configureEach {
                    versionMapping {
                        usage(Usage.JAVA_API) {
                            fromResolutionResult()
                        }
                        usage(Usage.JAVA_RUNTIME) {
                            fromResolutionResult()
                        }
                    }
                    customizePom()
                }
            }
        }
    }

    private fun MavenPublication.customizePom() {
        pom {
            description.set("Android test runner")
            url.set("https://github.com/badoo/marathon")
            withXml {
                // Avoid publishing BOM dependencies
                val node = asElement()
                val children = node.getElementsByTagName("dependencyManagement")
                for (i in 0 until children.length) {
                    node.removeChild(children.item(i))
                }
            }

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            scm {
                url.set("https://github.com/badoo/marathon")
            }
        }
    }

    private fun getMarathonVersion(providers: ProviderFactory, versionCatalog: VersionCatalog): String {
        val version = providers.environmentVariable("DEPLOY_VERSION_OVERRIDE")
            .orElse(versionCatalog.findVersion("marathon").get().requiredVersion)
        val releaseMode = providers.gradleProperty("releaseMode")
        val versionSuffix = if (releaseMode.orNull == "RELEASE") "" else "-SNAPSHOT"
        return version.get() + versionSuffix
    }

    companion object {
        private val JVM_TARGET = JavaLanguageVersion.of(17)
    }
}
