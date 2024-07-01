plugins {
    idea
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("com.github.gmazzo.buildconfig")
    jacoco
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

buildConfig {
    buildConfigField("String", "VERSION", "\"${Versions.marathon}\"")
}

dependencies {
    implementation(project(":report:html-report"))
    implementation(project(":report:execution-timeline"))

    implementation(Libraries.allure)
    implementation(Libraries.allureEnvironment)

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
    api(Libraries.koin)
    testImplementation(project(":vendor:vendor-test"))
    testImplementation(TestLibraries.kotlinCoroutinesTest)
    testImplementation(TestLibraries.testContainers)
    testImplementation(TestLibraries.ktorClientMock)
    testImplementation(TestLibraries.koin)
}

tasks.named<JacocoReport>("jacocoTestReport").configure {
    reports.xml.required.set(true)
    reports.html.required.set(true)
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
