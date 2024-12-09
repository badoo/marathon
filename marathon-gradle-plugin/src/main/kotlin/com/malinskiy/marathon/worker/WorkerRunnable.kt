package com.malinskiy.marathon.worker

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.execution.ComponentInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.gradle.api.GradleException

internal class WorkerRunnable(
    private val marathon: Marathon,
    private val componentsChannel: Channel<ComponentInfo>
) : Runnable {

    override fun run() = runBlocking {
        marathon.start()

        for (component in componentsChannel) {
            marathon.scheduleTests(component)
        }

        stopAndWaitForCompletion(marathon)
    }

    private suspend fun stopAndWaitForCompletion(marathon: Marathon) {
        val success = marathon.stopAndWaitForCompletion()

        val shouldReportFailure = !marathon.configuration.ignoreFailures
        if (!success && shouldReportFailure) {
            throw GradleException("Tests failed! See ${marathon.configuration.outputDir}/html/index.html")
        }
    }
}
