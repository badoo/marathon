plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(Libraries.kotlinCoroutines)
    implementation(Libraries.kotlinLogging)
    implementation(Libraries.kotlinReflect)
    implementation(Libraries.slf4jAPI)
    implementation(Libraries.logbackClassic)
    implementation(Libraries.ddPlist)
    implementation(Libraries.guava)
    implementation(Libraries.rsync4j)
    implementation(Libraries.sshj)
    implementation(Libraries.gson)
    implementation(Libraries.jacksonKotlin)
    implementation(Libraries.jacksonYaml)
    implementation(Libraries.jansi)
    implementation(project(":core"))
    implementation(Libraries.apacheCommonsText)
    testImplementation(TestLibraries.testContainers)
}

Deployment.initialize(project)
Testing.configure(project)
