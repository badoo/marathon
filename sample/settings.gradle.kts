pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
    plugins {
        val androidPluginVersion = "8.10.1"
        id("com.android.application") version androidPluginVersion
        id("com.android.library") version androidPluginVersion
        id("com.android.settings") version androidPluginVersion
        id("org.jetbrains.kotlin.android") version "2.1.21"
    }
    includeBuild("..")
}

plugins {
    id("com.android.settings")
}

android {
    compileSdk = 35
    minSdk = 21
    targetSdk = 35
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
