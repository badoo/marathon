pluginManagement {
    repositories {
        maven { url = uri("$rootDir/build/repository") }
        gradlePluginPortal()
    }
    plugins {
        id("com.github.gmazzo.buildconfig") version "3.0.3"
        id("org.jetbrains.kotlin.jvm") version "1.5.31"
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
