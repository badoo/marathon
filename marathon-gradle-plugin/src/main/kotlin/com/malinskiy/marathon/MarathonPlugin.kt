package com.malinskiy.marathon

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.Component
import com.android.build.api.variant.GeneratesTestApk
import com.android.build.api.variant.TestVariant
import com.android.build.api.variant.Variant
import com.malinskiy.marathon.android.findAdbPath
import com.malinskiy.marathon.worker.MarathonWorker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class MarathonPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.path == Project.PATH_SEPARATOR) {
            project.configureRootProject()
        } else {
            var androidPluginApplied = false
            project.plugins.withId("com.android.base") {
                androidPluginApplied = true
                project.configureAndroidProject()
            }
            project.afterEvaluate {
                check(androidPluginApplied) { "Android plugin is not applied" }
            }
        }
    }

    private fun Project.configureRootProject() {
        val marathonConfig = extensions.create<MarathonExtension>(MarathonExtension.NAME)
        marathonConfig.initDefaults()

        tasks.register<MarathonWorkerRunTask>(WORKER_TASK_NAME)

        gradle.projectsEvaluated {
            val outputDir = layout.buildDirectory.dir("reports/marathon").get().asFile
            val configuration = createCommonConfiguration(marathonConfig, findAdbPath(projectDir), outputDir)
            MarathonWorker.initialize(configuration)
        }
    }

    private fun Project.configureAndroidProject() {
        val marathonTask = tasks.register(TASK_PREFIX) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs all the instrumentation test variations on all the connected devices"
        }

        val marathonWorkerTask = rootProject.tasks.named<MarathonWorkerRunTask>(WORKER_TASK_NAME)
        val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            variant.components
                .filter { it is GeneratesTestApk }
                .forEach { component ->
                    val testTask = registerTestTask(variant, component, marathonWorkerTask)
                    marathonTask.configure { dependsOn(testTask) }
                }
        }
    }

    private fun Project.registerTestTask(
        variant: Variant,
        testComponent: Component,
        marathonWorkerTask: TaskProvider<MarathonWorkerRunTask>
    ): TaskProvider<MarathonScheduleTestsToWorkerTask> =
        tasks.register<MarathonScheduleTestsToWorkerTask>(variant.computeTaskName(TASK_PREFIX, "androidTest")) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs instrumentation tests on all the connected devices for '${variant.name}' " +
                "variation and generates a report with screenshots"

            componentName.set("${project.path}:${variant.name}")
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            testApkDir.set(testComponent.artifacts.get(SingleArtifact.APK))

            when (variant) {
                is ApplicationVariant -> testedApkDir.set(variant.artifacts.get(SingleArtifact.APK))
                is TestVariant -> testedApkDir.set(variant.testedApks)
            }

            finalizedBy(marathonWorkerTask)
        }

    companion object {
        /**
         * Task name prefix.
         */
        private const val TASK_PREFIX = "marathon"

        private const val WORKER_TASK_NAME = "marathonRun"
    }
}
