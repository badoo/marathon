plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
    implementation(libs.ktlint.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("marathonConventionsPlugin") {
            id = "com.badoo.marathon.conventions"
            implementationClass = "com.badoo.marathon.conventions.MarathonConventionsPlugin"
        }
    }
}
