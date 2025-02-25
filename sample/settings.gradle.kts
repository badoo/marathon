pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
    plugins {
        val androidPluginVersion = "8.8.1"
        id("com.android.application") version androidPluginVersion
        id("com.android.library") version androidPluginVersion
        id("com.android.settings") version androidPluginVersion
        id("org.jetbrains.kotlin.android") version "2.1.10"
    }
    includeBuild("..")
}

plugins {
    id("com.android.settings")
}

android {
    compileSdk = 34
    minSdk = 21
    targetSdk = 34
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "sample"

include(":app")
include(":library")
