package com.malinskiy.marathon

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.Component
import com.android.build.api.variant.GeneratesTestApk
import com.android.build.api.variant.TestVariant
import com.android.build.api.variant.Variant
import com.malinskiy.marathon.android.androidSdkLocation
import com.malinskiy.marathon.worker.MarathonWorker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized

class MarathonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project == project.rootProject) {
            project.configureRootProject()
        }

        val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java)
        if (androidComponents != null) {
            project.configureAndroidProject(androidComponents)
        } else if (project != project.rootProject) {
            throw IllegalStateException("Android plugin is not applied")
        }
    }

    private fun Project.configureRootProject() {
        val marathonConfig = project.extensions.create("marathon", MarathonExtension::class.java)
        tasks.register(WORKER_TASK_NAME, MarathonWorkerRunTask::class.java)

        gradle.projectsEvaluated {
            val configuration = createCommonConfiguration(project, marathonConfig, androidSdkLocation)
            MarathonWorker.initialize(configuration)
        }
    }

    private fun Project.configureAndroidProject(androidComponents: AndroidComponentsExtension<*, *, *>) {
        val marathonTask = tasks.register(TASK_PREFIX) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs all the instrumentation test variations on all the connected devices"
        }

        val marathonWorkerTask = rootProject.tasks.named(WORKER_TASK_NAME, MarathonWorkerRunTask::class.java)
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
        tasks.register("$TASK_PREFIX${variant.name.capitalized()}AndroidTest", MarathonScheduleTestsToWorkerTask::class.java) {
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
