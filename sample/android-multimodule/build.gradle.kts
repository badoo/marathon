plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("marathon")
}

allprojects {
    buildscript {
        repositories {
            mavenCentral()
            google()
            maven { url = uri("https://jitpack.io") }
        }
    }

    repositories {
        maven { url = uri("$rootDir/../build/repository") }
        mavenCentral()
        google()
    }
}
