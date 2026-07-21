package com.malinskiy.marathon

import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import com.malinskiy.marathon.android.AndroidComponentInfo
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "No outputs")
abstract class MarathonScheduleTestsToWorkerTask : DefaultTask() {
    @get:Input
    abstract val componentName: Property<String>

    @get:Input
    abstract val instrumentationRunnerArguments: MapProperty<String, String>

    @get:InputFiles
    abstract val testApkDir: DirectoryProperty

    @get:InputFiles
    @get:Optional
    abstract val testedApkDir: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:ServiceReference(MarathonBuildService.NAME)
    abstract val buildService: Property<MarathonBuildService>

    @TaskAction
    fun run() {
        val artifactsLoader = builtArtifactsLoader.get()
        val testApks = testApkDir.get().let { checkNotNull(artifactsLoader.load(it)) { "Unable to load test APKs from $it" } }
        val testedApks = testedApkDir.orNull?.let { checkNotNull(artifactsLoader.load(it)) { "Unable to load tested APKs from $it" } }

        val componentInfo = AndroidComponentInfo(
            name = componentName.get(),
            applicationId = testedApks?.applicationId,
            applicationOutput = testedApks?.singleFile,
            testApplicationId = testApks.applicationId,
            testApplicationOutput = testApks.singleFile,
            instrumentationArgs = instrumentationRunnerArguments.get()
        )
        logger.lifecycle(
            "Scheduling instrumentation tests ${componentInfo.testApplicationOutput}" +
                (componentInfo.applicationOutput?.let { " for app $it" }.orEmpty())
        )

        buildService.get().scheduleTests(componentInfo)
    }

    private val BuiltArtifacts.singleFile: File
        get() = checkNotNull(elements.singleOrNull()?.outputFile?.let { File(it) }) { "Marathon requires a single APK" }
}
