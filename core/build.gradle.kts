plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.badoo.marathon.conventions")
    id("dev.zacsweers.redacted")
}

dependencies {
    implementation(project(":report:html-report"))
    implementation(project(":report:execution-timeline"))

    implementation(libs.allure.java.commons)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.jackson.annotations)
    implementation(libs.slf4j.api)

    testImplementation(libs.assertj.core)
    testImplementation(libs.slf4j.simple)

    integrationTestImplementation(libs.assertj.core)
    integrationTestImplementation(libs.slf4j.simple)

    testFixturesImplementation(libs.jsonassert)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.ktor.client.core)
}

testing {
    suites {
        val integrationTest by getting(JvmTestSuite::class) {
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.gson)
            }
        }
    }
}
