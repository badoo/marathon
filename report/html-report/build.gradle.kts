plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(Libraries.gson)
    implementation(Libraries.kotlinCoroutines)
    implementation(Libraries.kotlinLogging)
}

Deployment.initialize(project)
