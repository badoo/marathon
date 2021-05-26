plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("marathon") apply false
}

allprojects {
    repositories {
        maven { url = uri("$rootDir/../build/repository") }
        mavenCentral()
        google()
    }
}
