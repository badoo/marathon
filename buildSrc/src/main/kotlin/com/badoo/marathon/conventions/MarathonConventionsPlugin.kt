package com.badoo.marathon.conventions

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.attributes.TestSuiteType
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.tasks.GenerateModuleMetadata
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.credentials
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.base.TestingExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class MarathonConventionsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val versionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

        project.group = "com.github.badoo.marathon"
        project.version = project.providers.gradleProperty("VERSION_NAME").get()

        project.plugins.withId("java") {
            project.plugins.apply("maven-publish")
            project.plugins.apply("java-test-fixtures")
            project.plugins.apply("jvm-test-suite")
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
                languageVersion.set(JavaLanguageVersion.of(17))
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

        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
            dependsOn(tasks.named("detektMain"))
            dependsOn(tasks.named("detektTest"))

            if (pluginManager.hasPlugin("java-test-fixtures")) {
                dependsOn(tasks.named("detektTestFixtures"))
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureTesting(versionCatalog: VersionCatalog) {
        extensions.configure<TestingExtension> {
            suites.run {
                named<JvmTestSuite>("test") {
                    useJUnitJupiter(versionCatalog.findVersion("junit5").get().requiredVersion)

                    dependencies {
                        implementation(versionCatalog.findLibrary("spek-api").get())
                        runtimeOnly(versionCatalog.findLibrary("spek-engine").get())
                    }
                }
                val integrationTest = register<JvmTestSuite>("integrationTest") {
                    testType.set(TestSuiteType.INTEGRATION_TEST)

                    dependencies {
                        implementation(project())
                        implementation(testFixtures(project()))
                    }
                }
                withType<JvmTestSuite>().configureEach {
                    dependencies {
                        implementation(platform(versionCatalog.findLibrary("kotlin-bom").get()))
                        implementation(platform(versionCatalog.findLibrary("kotlinx-coroutines-bom").get()))
                        implementation(platform(versionCatalog.findLibrary("ktor-bom").get()))
                        implementation(platform(versionCatalog.findLibrary("junit-bom").get()))
                        implementation(versionCatalog.findLibrary("mockito-kotlin").get())
                        implementation(versionCatalog.findLibrary("junit-jupiter-api").get())
                        implementation(versionCatalog.findLibrary("kotlinx-coroutines-test").get())
                        runtimeOnly(versionCatalog.findLibrary("junit-jupiter-engine").get())
                        runtimeOnly(versionCatalog.findLibrary("junit-platform-launcher").get())
                    }
                }

                tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
                    dependsOn(integrationTest)
                }
            }
        }

        dependencies {
            add("testFixturesImplementation", platform(versionCatalog.findLibrary("kotlinx-coroutines-bom").get()))
            add("testFixturesImplementation", platform(versionCatalog.findLibrary("ktor-bom").get()))
        }
    }

    private fun Project.configurePublishing() {
        configure<PublishingExtension> {
            repositories {
                val mavenUrl = providers.gradleProperty("internalMavenUrl")
                if (mavenUrl.isPresent) {
                    maven(mavenUrl) {
                        name = "internalMaven"
                        if (url.scheme != "file") {
                            credentials(PasswordCredentials::class)
                        }
                    }
                }
            }

            publications {
                if (!plugins.hasPlugin("java-gradle-plugin")) {
                    create<MavenPublication>("maven") {
                        from(components["java"])
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

                    if (plugins.hasPlugin("java-test-fixtures")) {
                        suppressPomMetadataWarningsFor("testFixturesApiElements")
                        suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
                    }
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
}
