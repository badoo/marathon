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
import kotlinx.coroutines.async
import java.io.File
import java.nio.file.Files.createDirectories
import java.nio.file.Paths
import kotlin.system.measureTimeMillis

class PullScreenshotTestRunListener(
    private val device: AndroidDevice,
    private val devicePoolId: DevicePoolId,
    private val outputDir: File,
    private val testBatch: TestBatch,
    private val pullScreenshotFilterConfiguration: FilteringConfiguration,
    private val coroutineScope: CoroutineScope
) : TestRunListener {

    private val logger = MarathonLogging.logger("PullScreenshot")

    private var screenshotDeferred: Deferred<Unit>? = null

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        super.testRunEnded(elapsedTime, runMetrics)

        screenshotDeferred?.cancel()
        if (shouldRunPullScreenshot()) {
            screenshotDeferred = coroutineScope.async {
                val componentInfo = testBatch.componentInfo as AndroidComponentInfo
                val applicationId = componentInfo.applicationId ?: componentInfo.testApplicationId
                pullScreenshots(applicationId)
                removeScreenshots(applicationId)
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
        val remoteDir = device.fileManager.getScreenshotsDir(applicationId)
        val outputDir = outputDirectory.toFile()

        try {
            val millis = measureTimeMillis {
                createDirectories(outputDirectory)
                device.fileManager.pullFromFilesDir(
                    applicationId = applicationId,
                    remoteDir = remoteDir,
                    localDir = outputDir
                )
            }
            logger.trace { "Pulling screenshots finished in ${millis}ms from $remoteDir to $outputDir" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to pull screenshots from $remoteDir" }
        }
    }

    private fun removeScreenshots(applicationId: String) {
        val remoteDir = device.fileManager.getScreenshotsDir(applicationId)
        device.fileManager.removeFromFilesDir(applicationId, remoteDir)
    }

    companion object {
        private const val OUTPUT_FOLDER_NAME = "ui-screenshot"
    }
}
