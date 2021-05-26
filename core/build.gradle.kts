import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    idea
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    jacoco
    id("de.fuerstenau.buildconfig") version "1.1.8"
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets["main"].output
        compileClasspath += sourceSets["test"].output
        compileClasspath += configurations.testCompileClasspath

        runtimeClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["test"].output
        runtimeClasspath += configurations.testRuntimeClasspath
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDirs("src/integrationTest/kotlin")
        }
    }
}

buildConfig {
    appName = project.name
    version = Versions.marathon
    buildConfigField("String", "BUGSNAG_TOKEN", System.getenv("BUGSNAG_TOKEN"))
}

dependencies {
    implementation(project(":report:html-report"))
    implementation(project(":report:execution-timeline"))

    implementation(Libraries.allure)
    implementation(Libraries.allureEnvironment)

    implementation(project(":analytics:usage"))
    implementation(Libraries.ktorClient)
    implementation(Libraries.ktorAuth)
    implementation(Libraries.ktorApacheClient)
    implementation(Libraries.gson)
    implementation(Libraries.jacksonAnnotations)
    implementation(Libraries.apacheCommonsText)
    implementation(Libraries.apacheCommonsIO)
    implementation(Libraries.apacheCommonsCollections)
    implementation(Libraries.kotlinCoroutines)
    implementation(Libraries.kotlinLogging)
    implementation(Libraries.slf4jAPI)
    implementation(Libraries.logbackClassic)
    implementation(Libraries.influxDbClient)
    api(Libraries.koin)
    api(Libraries.bugsnag)
    testImplementation(project(":vendor:vendor-test"))
    testImplementation(TestLibraries.kotlinCoroutinesTest)
    testImplementation(TestLibraries.testContainers)
    testImplementation(TestLibraries.testContainersInflux)
    testImplementation(TestLibraries.ktorClientMock)
    testImplementation(TestLibraries.koin)
}

tasks.named<JacocoReport>("jacocoTestReport").configure {
    reports.xml.isEnabled = true
    reports.html.isEnabled = true
    dependsOn(tasks.named("test"))
}

val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath

    exclude("**/resources/**")

    shouldRunAfter("test")
}

Deployment.initialize(project)
Testing.configure(project)
