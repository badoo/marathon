plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("org.jetbrains.dokka")
}

gradlePlugin {
    plugins {
        create("marathonPlugin") {
            id = "com.badoo.marathon"
            implementationClass = "com.malinskiy.marathon.MarathonPlugin"
        }
    }
}

Deployment.initialize(project)

dependencies {
    implementation(gradleApi())
    implementation(Libraries.kotlinLogging)
    implementation(Libraries.kotlinCoroutines)
    implementation(project(":core"))
    implementation(project(":vendor:vendor-android:base"))
    implementation(project(":vendor:vendor-android:ddmlib"))
    implementation(Libraries.androidCommon)
    implementation(BuildPlugins.androidGradle)
}
