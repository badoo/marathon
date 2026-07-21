plugins {
    id("org.jetbrains.kotlinx.kover")
    id("com.badoo.marathon.conventions")
}

dependencies {
    kover(project(":core"))
    kover(project(":vendor:vendor-android:base"))
    kover(project(":vendor:vendor-android:ddmlib"))
    kover(project(":marathon-gradle-plugin"))
    kover(project(":report:html-report"))
    kover(project(":report:execution-timeline"))
}
