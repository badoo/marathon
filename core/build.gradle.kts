plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.badoo.marathon.conventions")
}

dependencies {
    implementation(project(":report:html-report"))
    implementation(project(":report:execution-timeline"))

    implementation(libs.allure.java.commons)
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

    testFixturesImplementation(libs.ktor.client.core)
}

testing {
    suites {
        val integrationTest by getting(JvmTestSuite::class) {
            dependencies {
                implementation(project(":vendor:vendor-test"))
                implementation(libs.ktor.client.mock)
                implementation(libs.testcontainers)
                implementation(libs.gson)
            }
        }
    }
}
