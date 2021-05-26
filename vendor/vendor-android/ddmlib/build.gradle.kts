plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(Libraries.allure)
    implementation(Libraries.kotlinCoroutines)
    implementation(Libraries.kotlinLogging)
    implementation(Libraries.ddmlib)
    implementation(Libraries.dexTestParser)
    implementation(Libraries.axmlParser)
    implementation(Libraries.jacksonAnnotations)
    implementation(Libraries.scalr)
    implementation(Libraries.apacheCommonsIO)
    implementation(project(":core"))
    implementation(project(":vendor:vendor-android:base"))
    implementation(Libraries.logbackClassic)
    testImplementation(project(":vendor:vendor-test"))
    testImplementation(TestLibraries.koin)
}

Deployment.initialize(project)
Testing.configure(project)
