plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.badoo.marathon.conventions")
}

gradlePlugin {
    plugins {
        create("marathon") {
            id = "com.badoo.marathon"
            implementationClass = "com.malinskiy.marathon.MarathonPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(project(":core"))
    implementation(project(":vendor:vendor-android:base"))
    implementation(project(":vendor:vendor-android:ddmlib"))
    implementation(libs.android.gradle.api)
    implementation(libs.android.tools.common)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.coroutines.core)
}
