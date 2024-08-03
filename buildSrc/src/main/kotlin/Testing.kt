import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

object Testing {
    fun configure(project: Project) {
        project.dependencies {
            add("testImplementation", TestLibraries.kluent)
            add("testImplementation", TestLibraries.mockitoKotlin)
            add("testImplementation", TestLibraries.spekAPI)
            add("testImplementation", TestLibraries.junit5)
            add("testImplementation", TestLibraries.junitJupiterApi)
            add("testRuntimeOnly", TestLibraries.junitJupiterEngine)
            add("testRuntimeOnly", TestLibraries.spekJUnitPlatformEngine)
        }

        project.tasks.withType<Test>().configureEach {
            useJUnitPlatform {
                includeEngines("spek", "junit-jupiter")
            }
        }
    }
}
