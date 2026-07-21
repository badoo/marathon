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

    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":vendor:vendor-android:base")))
    testImplementation(libs.assertj.core)
    testImplementation(libs.slf4j.simple)

    testFixturesImplementation(project(":core"))
    testFixturesImplementation(project(":vendor:vendor-android:base"))
    testFixturesImplementation(libs.android.tools.ddmlib)
    testFixturesImplementation(libs.mockito.kotlin)
}
