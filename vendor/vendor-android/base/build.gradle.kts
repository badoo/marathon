plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.badoo.marathon.conventions")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.allure.java.commons)
    implementation(libs.android.tools.common)
    implementation(libs.axmlparser)
    implementation(libs.dextestparser)
    implementation(libs.imgscalr)
    implementation(libs.jackson.annotations)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)

    testImplementation(project(":vendor:vendor-test"))
    testImplementation(libs.koin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
