import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("io.gitlab.arturbosch.detekt") apply false
}

allprojects {
    group = "com.github.badoo.marathon"

    plugins.withId("org.jetbrains.kotlin.jvm") {
        plugins.apply("io.gitlab.arturbosch.detekt")

        dependencies.add("implementation", dependencies.platform(Libraries.kotlinBom))
        dependencies.add("implementation", dependencies.platform(Libraries.kotlinCoroutinesBom))
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            withJavadocJar()
            withSourcesJar()
        }
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.from("$rootDir/detekt.yml")
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
