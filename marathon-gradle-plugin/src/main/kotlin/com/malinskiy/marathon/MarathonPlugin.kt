package com.malinskiy.marathon

import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.TestVariant
import com.malinskiy.marathon.android.androidSdkLocation
import com.malinskiy.marathon.worker.MarathonWorker
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider

class MarathonPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project == project.rootProject) {
            project.configureRootProject()
        }

        project.plugins.withId("com.android.application") {
            project.configureAndroidProject()
        }
        project.plugins.withId("com.android.library") {
            project.configureAndroidProject()
        }
    }

    private fun Project.configureRootProject() {
        val marathonConfig = project.extensions.create(EXTENSION_NAME, MarathonExtension::class.java)
        tasks.register(WORKER_TASK_NAME, MarathonWorkerRunTask::class.java)

        gradle.projectsEvaluated {
            val configuration = createCommonConfiguration(project, marathonConfig, androidSdkLocation)
            MarathonWorker.initialize(configuration)
        }
    }

    private fun Project.configureAndroidProject() {
        val testedExtension = extensions.findByType(TestedExtension::class.java)
            ?: throw IllegalStateException("Android extension not found")

        val marathonWorkerTask = rootProject.tasks.named(WORKER_TASK_NAME, MarathonWorkerRunTask::class.java)

        val marathonTask = tasks.register(TASK_PREFIX) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs all the instrumentation test variations on all the connected devices"
        }

        testedExtension.testVariants.all {
            val testTaskForVariant = project.registerTaskForTestVariant(this, marathonWorkerTask)
            marathonTask.configure { dependsOn(testTaskForVariant) }
        }
    }

    private fun Project.registerTaskForTestVariant(
        variant: TestVariant,
        marathonWorkerTask: TaskProvider<MarathonWorkerRunTask>
    ): TaskProvider<MarathonScheduleTestsToWorkerTask> {
        checkTestVariants(variant)

        val marathonTask = tasks.register("$TASK_PREFIX${variant.name.capitalize()}", MarathonScheduleTestsToWorkerTask::class.java) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs instrumentation tests on all the connected devices for '${variant.name}' " +
                "variation and generates a report with screenshots"
            outputs.upToDateWhen { false }
            dependsOn(variant.testedVariant.assembleProvider, variant.assembleProvider)
        }

        variant.testedVariant.outputs.all {
            checkTestedVariants(this)

            marathonTask.configure {
                componentInfo = createComponentInfo(
                    project = project,
                    flavorName = variant.name,
                    applicationVariant = variant.testedVariant,
                    testVariant = variant
                )
                finalizedBy(marathonWorkerTask)
            }
        }

        return marathonTask
    }

    private fun checkTestVariants(testVariant: TestVariant) {
        if (testVariant.outputs.size > 1) {
            throw UnsupportedOperationException("The Marathon plugin does not support abi/density splits for test APKs")
        }
    }

    /**
     * Checks that if the base variant contains more than one outputs (and has therefore splits), it is the universal APK.
     * Otherwise, we can test the single output. This is a workaround until Fork supports test & app splits properly.
     *
     * @param baseVariant the tested variant
     */
    private fun checkTestedVariants(baseVariantOutput: BaseVariantOutput) {
        if (baseVariantOutput.outputs.size > 1) {
            throw UnsupportedOperationException(
                "The Marathon plugin does not support abi splits for app APKs, " +
                    "but supports testing via a universal APK. "
                    + "Add the flag \"universalApk true\" in the android.splits.abi configuration."
            )
        }
    }

    companion object {
        /**
         * Task name prefix.
         */
        private const val TASK_PREFIX = "marathon"

        private const val EXTENSION_NAME = "marathon"
        private const val WORKER_TASK_NAME = "marathonRun"
    }
}
