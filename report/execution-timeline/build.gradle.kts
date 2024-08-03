plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(Libraries.gson)
    implementation(Libraries.kotlinCoroutines)
    implementation(Libraries.kotlinLogging)
}

Deployment.initialize(project)
