pluginManagement {
    repositories {
        google()
        maven { url = uri("$rootDir/../../build/repository") }
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
    plugins {
        id("com.android.application") version "4.2.1"
        id("com.android.library") version "4.2.1"
        id("org.jetbrains.kotlin.android") version "1.4.32"
        id("marathon") version "0.5.4-SNAPSHOT"
    }
}

rootProject.name = "multi-module"
include("app")
include("library")
