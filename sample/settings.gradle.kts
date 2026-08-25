pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
    plugins {
        val androidPluginVersion = "9.3.2"
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
    compileSdk = 36
    minSdk = 23
    targetSdk = 36
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
