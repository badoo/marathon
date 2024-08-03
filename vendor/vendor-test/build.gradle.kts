plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.badoo.marathon.conventions")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.jsonassert)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.reflect)
    implementation(libs.spek.api)
}
