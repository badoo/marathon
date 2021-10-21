package com.malinskiy.marathon.android.executor.listeners.pull

import com.malinskiy.marathon.android.AndroidComponentInfo
import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.FilteringConfiguration
import com.malinskiy.marathon.execution.matches
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.newFixedThreadPoolContext
import java.io.File
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

class PullScreenshotTestRunListener(
    private val device: AndroidDevice,
    private val devicePoolId: DevicePoolId,
    private val outputDir: File,
    private val testBatch: TestBatch,
    private val parentJob: Job,
    private val pullScreenshotFilterConfiguration: FilteringConfiguration
) : TestRunListener, CoroutineScope {

    private val logger = MarathonLogging.logger("PullScreenshot")

    private val threadPoolDispatcher by lazy {
        newFixedThreadPoolContext(1, "PullScreenshot - ${device.serialNumber}")
    }
    override val coroutineContext: CoroutineContext
        get() = threadPoolDispatcher
    private var screenshotDeferred: Deferred<Unit>? = null

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        super.testRunEnded(elapsedTime, runMetrics)

        screenshotDeferred?.cancel()
        if (shouldRunPullScreenshot()) {
            screenshotDeferred = async(parentJob) {
                val componentInfo = testBatch.componentInfo as AndroidComponentInfo
                pullScreenshots(componentInfo.testApplicationId)
                removeScreenshots(componentInfo.testApplicationId)
            }
        }
    }

    private fun shouldRunPullScreenshot(): Boolean =
        testBatch.tests.any { testFromBatch -> testFromBatch.matchWhitelist() }

    private fun Test.matchWhitelist() =
        pullScreenshotFilterConfiguration.whitelist.any { filter -> filter.matches(this) }

    private fun pullScreenshots(applicationId: String) {
        val deviceInfo = device.toDeviceInfo()

        val outputDirectory = Paths.get(
            outputDir.absolutePath,
            OUTPUT_FOLDER_NAME,
            devicePoolId.name,
            deviceInfo.serialNumber,
            testBatch.id
        )
        val remoteFilePath = device.fileManager.getScreenshotsDir(applicationId)
        val outputPath = outputDirectory.toFile()

        val millis = measureTimeMillis {
            createDirectories(outputDirectory)

            device.fileManager.pullFromFilesDir(
                applicationId = applicationId,
                remoteFilePath = remoteFilePath,
                localDir = outputPath,
                fileMatch = listOf("metadata.json", "*.png")
            )
        }
        logger.trace { "Pulling screenshots finished in ${millis}ms from $remoteFilePath to $outputPath" }
    }

    private fun removeScreenshots(applicationId: String) {
        val remoteFilePath = device.fileManager.getScreenshotsDir(applicationId)
        val millis = measureTimeMillis {
            device.fileManager.removeFromFilesDir(applicationId, remoteFilePath)
        }
        logger.trace { "Removed files in ${millis}ms from $remoteFilePath" }
    }

    companion object {
        private const val OUTPUT_FOLDER_NAME = "ui-screenshot"
    }
}
