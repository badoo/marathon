plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.badoo.marathon.conventions")
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output
        compileClasspath += sourceSets["test"].output
        compileClasspath += configurations.testCompileClasspath.get()

        runtimeClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["test"].output
        runtimeClasspath += configurations.testRuntimeClasspath.get()
    }
}

dependencies {
    implementation(project(":report:html-report"))
    implementation(project(":report:execution-timeline"))

    implementation(libs.allure.java.commons)
    implementation(libs.allure.environment.writer)
    implementation(libs.apache.commons.collections)
    implementation(libs.apache.commons.io)
    implementation(libs.apache.commons.text)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.apache)
    implementation(libs.jackson.annotations)
    implementation(libs.slf4j.api)
    api(libs.koin.core)

    testImplementation(project(":vendor:vendor-test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.koin.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.testcontainers)
}

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = JavaBasePlugin.VERIFICATION_GROUP

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath

    exclude("**/resources/**")

    shouldRunAfter(tasks.named("test"))
}

tasks.named("check") {
    dependsOn(integrationTest)
}
