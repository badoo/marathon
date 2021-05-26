import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    debug = true
    version = "1.0.0.RC6-4"
    profile = "main"

    profile("main", Action {
        input = rootProject.projectDir.absolutePath
        filters = ".*/resources/.*,.*/build/.*,.*/sample-app/.*"
        config = "${rootProject.projectDir}/default-detekt-config.yml"
        baseline = "${rootProject.projectDir}/reports/baseline.xml"
    })
}

allprojects {
    group = "com.malinskiy.marathon"

    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }

    project.plugins.withId("org.jetbrains.kotlin.jvm") {
        project.dependencies.add("implementation", project.dependencies.platform(Libraries.kotlinBom))
    }

    project.tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            apiVersion = "1.4"
            jvmTarget = "1.8"
            freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
}

tasks.register<Delete>("clean") {
    delete(project.buildDir)
}
