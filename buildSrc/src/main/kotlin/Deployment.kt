import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate

object Deployment {
    var releaseMode: String? = null
    var versionSuffix: String? = null

    fun initialize(project: Project) {
        val releaseMode: String? by project
        val versionSuffix = when (releaseMode) {
            "RELEASE" -> ""
            else -> "-SNAPSHOT"
        }

        Deployment.releaseMode = releaseMode
        Deployment.versionSuffix = versionSuffix

        initializePublishing(project)
    }

    private fun initializePublishing(project: Project) {
        project.version = Versions.marathon + versionSuffix

        project.plugins.apply("maven-publish")

        project.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("default") {
                    Deployment.customizePom(project, pom)
                    from(project.components["java"])
                }
            }
        }
    }

    fun customizePom(project: Project, pom: MavenPom?) {
        pom?.apply {
            name.set(project.name)
            url.set("https://github.com/badoo/marathon")
            description.set("Android test runner")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("marathon-team")
                    name.set("Marathon team")
                    email.set("anton@malinskiy.com")
                }
            }

            scm {
                url.set("https://github.com/badoo/marathon")
            }
        }
    }
}
