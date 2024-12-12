plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.badoo.marathon.conventions")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
}
