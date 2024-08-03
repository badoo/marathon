pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "marathon"

include("core")
include("vendor:vendor-android:base")
include("vendor:vendor-android:ddmlib")
include("vendor:vendor-test")
include("marathon-gradle-plugin")
include("report:html-report")
include("report:execution-timeline")
