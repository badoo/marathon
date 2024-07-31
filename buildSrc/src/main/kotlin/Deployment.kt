import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension
import java.net.URI

object Deployment {
    val githubUser = System.getenv("GITHUB_MAVEN_USERNAME")
    val githubPassword = System.getenv("GITHUB_MAVEN_PASSWORD")
    var releaseMode: String? = null
    var versionSuffix: String? = null

    val githubDeployUrl = "https://maven.pkg.github.com/badoo"

    fun initialize(project: Project) {
        val releaseMode: String? by project
        val versionSuffix = when (releaseMode) {
            "RELEASE" -> ""
            else -> "-SNAPSHOT"
        }

        Deployment.releaseMode = releaseMode
        Deployment.versionSuffix = versionSuffix

        initializePublishing(project)
        initializeSigning(project)
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
            repositories {
                maven {
                    name = "GitHub"
                    credentials {
                        username = Deployment.githubUser
                        password = Deployment.githubPassword
                    }
                    url = URI.create(Deployment.githubDeployUrl)
                }
            }
        }
    }

    private fun initializeSigning(project: Project) {
        val passphrase = System.getenv("GPG_PASSPHRASE")
        passphrase?.let {
            project.plugins.apply("signing")

            val publishing = project.the(PublishingExtension::class)
            project.configure<SigningExtension> {
                sign(publishing.publications.getByName("default"))
            }

            project.extra.set("signing.keyId", "1131CBA5")
            project.extra.set("signing.password", passphrase)
            project.extra.set("signing.secretKeyRingFile", "${project.rootProject.rootDir}/.buildsystem/secring.gpg")
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
