pluginManagement {
    repositories {
        maven { url = uri("$rootDir/build/repository") }
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.4.32"
        id("org.jetbrains.dokka") version "0.9.17"
        id("io.gitlab.arturbosch.detekt") version "1.0.0.RC6-4"
    }
}

rootProject.name = "marathon"
include("core")
include("vendor:vendor-android:base")
include("vendor:vendor-android:ddmlib")
include("vendor:vendor-android:adam")
include("vendor:vendor-ios")
include("vendor:vendor-test")
include("marathon-gradle-plugin")
include("report:html-report")
include("report:execution-timeline")
include("cli")
include(":analytics:usage")
//include("vendor:adam")
