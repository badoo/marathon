plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.badoo.marathon.conventions")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":vendor:vendor-android:base"))
    implementation(libs.allure.java.commons)
    implementation(libs.android.tools.common)
    implementation(libs.android.tools.ddmlib)
    implementation(libs.apache.commons.io)
    implementation(libs.axmlparser)
    implementation(libs.dextestparser)
    implementation(libs.imgscalr)
    implementation(libs.jackson.annotations)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)

    testImplementation(project(":vendor:vendor-test"))
    testImplementation(libs.slf4j.simple)
}
