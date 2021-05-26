plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

Deployment.initialize(project)

dependencies {
    implementation(Analytics.googleAnalyticsWrapper)
}
