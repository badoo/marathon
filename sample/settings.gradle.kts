pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
    plugins {
        val androidPluginVersion = "9.1.1"
        id("com.android.application") version androidPluginVersion
        id("com.android.library") version androidPluginVersion
        id("com.android.settings") version androidPluginVersion
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
    includeBuild("..")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
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
