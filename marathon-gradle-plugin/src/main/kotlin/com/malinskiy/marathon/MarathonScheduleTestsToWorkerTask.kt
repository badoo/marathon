package com.malinskiy.marathon

import com.android.build.api.variant.BuiltArtifactsLoader
import com.malinskiy.marathon.android.AndroidComponentInfo
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.worker.MarathonWorker
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

private val log = MarathonLogging.logger {}

abstract class MarathonScheduleTestsToWorkerTask : DefaultTask() {
    @get:Input
    abstract val componentName: Property<String>

    @get:Input
    @get:Optional
    abstract val applicationId: Property<String>

    @get:Input
    abstract val testApplicationId: Property<String>

    @get:InputFiles
    @get:Optional
    abstract val applicationApkDir: DirectoryProperty

    @get:InputFiles
    abstract val testApkDir: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @TaskAction
    fun run() {
        val componentInfo = AndroidComponentInfo(
            name = componentName.get(),
            applicationId = applicationId.orNull,
            testApplicationId = testApplicationId.get(),
            applicationOutput = applicationApkDir.orNull?.let { findApk(it) },
            testApplicationOutput = findApk(testApkDir.get()),
        )
        log.info { "Scheduling instrumentation tests ${componentInfo.testApplicationOutput} for app ${componentInfo.applicationOutput}" }

        MarathonWorker.ensureStarted()
        MarathonWorker.scheduleTests(componentInfo)
    }

    private fun findApk(apkDirectory: Directory): File {
        val artifacts = builtArtifactsLoader.get().load(apkDirectory) ?: throw RuntimeException("Cannot load APKs")
        when {
            artifacts.elements.size > 1 -> throw IllegalStateException("Marathon plugin does not support ABI splits")
            artifacts.elements.isEmpty() -> throw IllegalStateException("No APKs for variant ${componentName.get()}")
        }
        return File(artifacts.elements.first().outputFile)
    }
}
