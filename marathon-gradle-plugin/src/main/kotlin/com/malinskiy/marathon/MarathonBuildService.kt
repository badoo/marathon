package com.malinskiy.marathon

import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.worker.WorkerContext
import com.malinskiy.marathon.worker.WorkerHandler
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class MarathonBuildService :
    BuildService<MarathonBuildService.Params>,
    WorkerHandler {
    private val lazyWorkerContext = lazy {
        val marathonExtension = parameters.marathonConfig.get()
        MarathonLogging.debug = marathonExtension.debug.get()
        val configuration = createCommonConfiguration(
            marathonExtension,
            parameters.adbPath.get().asFile,
            parameters.outputDir.get().asFile,
            parameters.tempDir.get().asFile,
        )
        WorkerContext(configuration)
    }

    override fun scheduleTests(componentInfo: ComponentInfo) {
        lazyWorkerContext.value.scheduleTests(componentInfo)
    }

    override fun await() {
        if (lazyWorkerContext.isInitialized()) {
            lazyWorkerContext.value.await()
        }
    }

    override fun close() {
        if (lazyWorkerContext.isInitialized()) {
            lazyWorkerContext.value.close()
        }
    }

    interface Params : BuildServiceParameters {
        val adbPath: DirectoryProperty
        val outputDir: DirectoryProperty
        val tempDir: DirectoryProperty
        val marathonConfig: Property<MarathonExtension>
    }

    companion object {
        const val NAME = "marathon"
    }
}
