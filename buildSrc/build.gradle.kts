plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("marathonConventionsPlugin") {
            id = "com.badoo.marathon.conventions"
            implementationClass = "com.badoo.marathon.conventions.MarathonConventionsPlugin"
        }
    }
}
