plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(Libraries.kotlinCoroutines)
    implementation(Libraries.kotlinLogging)
    implementation(Libraries.kotlinReflect)
    implementation(TestLibraries.jsonAssert)
    implementation(TestLibraries.spekAPI)
    implementation(TestLibraries.kluent)
    implementation(TestLibraries.mockitoKotlin)
    implementation(project(":core"))
}
