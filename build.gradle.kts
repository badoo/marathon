import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    debug = true
    version = "1.0.1"

    input = files(rootProject.projectDir.absolutePath)
    filters = ".*/resources/.*,.*/build/.*,.*/sample-app/.*"
    config = files("${rootProject.projectDir}/default-detekt-config.yml")
    baseline = file("${rootProject.projectDir}/reports/baseline.xml")
}

allprojects {
    group = "com.github.badoo.marathon"

    plugins.withId("org.jetbrains.kotlin.jvm") {
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

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
