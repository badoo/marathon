package com.malinskiy.marathon.android.executor.listeners.video

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.exception.TransferException
import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.attachment.AttachmentListener
import com.malinskiy.marathon.report.attachment.AttachmentProvider
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds

class ScreenRecorderTestRunListener(
    private val attachmentManager: AttachmentManager,
    private val device: AndroidDevice,
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) : TestRunListener,
    AttachmentProvider {

    private val attachmentListeners = mutableListOf<AttachmentListener>()

    private val logger = MarathonLogging.getLogger(ScreenRecorderTestRunListener::class.java)

    private val screenRecorderStopper = ScreenRecorderStopper(device)

    private var hasFailed: Boolean = false
    private var screenRecorder: ScreenRecorder? = null

    override fun registerListener(listener: AttachmentListener) {
        attachmentListeners.add(listener)
    }

    override fun testStarted(test: Test) {
        hasFailed = false

        screenRecorder?.stop()
        screenRecorder = ScreenRecorder(device, device.fileManager.remoteVideoForTest(test), ioDispatcher)
            .apply { start(coroutineScope) }
    }

    override fun testFailed(test: Test, trace: String) {
        hasFailed = true
    }

    override fun testAssumptionFailure(test: Test, trace: String) {
        screenRecorder?.stop()
        pullVideo(test)
    }

    override fun testEnded(test: Test, testMetrics: Map<String, String>) {
        screenRecorder?.stop()
        pullVideo(test)
    }

    override fun testRunFailed(errorMessage: String) {
        screenRecorder?.stop()
    }

    override fun testRunStopped(elapsedTime: Long) {
        screenRecorder?.stop()
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        screenRecorder?.stop()
    }

    private fun pullVideo(test: Test) {
        val screenRecorder = screenRecorder ?: return
        try {
            val joinMillis = measureTimeMillis {
                screenRecorder.await(1.seconds)
            }
            logger.trace("[{}] Awaited screen recording in {}ms", device.serialNumber, joinMillis)
            if (screenRecorder.isCancelled) {
                return
            }
            if (hasFailed) {
                stopAndPullVideo(test)
            }
            removeTestVideo(test)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("[{}] Interrupted while stopping screen recording and pulling video", device.serialNumber, e)
        } catch (e: TransferException) {
            logger.warn("[{}] Failed to pull video", device.serialNumber, e)
        }
    }

    private fun stopAndPullVideo(test: Test) {
        val stopMillis = measureTimeMillis {
            screenRecorderStopper.stopScreenRecord()
        }
        logger.trace("[{}] Stopped screen recording in {}ms", device.serialNumber, stopMillis)
        pullTestVideo(test)?.let { attachment ->
            attachmentListeners.forEach { it.onAttachment(test, attachment) }
        }
    }

    private fun pullTestVideo(test: Test): Attachment? {
        val attachment = attachmentManager.createAttachment(
            FileType.VIDEO,
            AttachmentType.VIDEO,
        )
        val localVideoFile = attachment.file
        val remoteFilePath = device.fileManager.remoteVideoForTest(test)
        val millis = measureTimeMillis {
            device.fileManager.pullFile(remoteFilePath, localVideoFile)
        }
        logger.trace("[{}] Pulling video finished in {}ms {}", device.serialNumber, millis, remoteFilePath)

        if (localVideoFile.length() == 0L) {
            logger.debug("[{}] Skipping empty video for {}", device.serialNumber, test.toSimpleSafeTestName())
            return null
        }
        return attachment
    }

    private fun removeTestVideo(test: Test) {
        val remoteFilePath = device.fileManager.remoteVideoForTest(test)
        val millis = measureTimeMillis {
            device.fileManager.remove(remoteFilePath)
        }
        logger.trace("[{}] Removed file in {}ms {}", device.serialNumber, millis, remoteFilePath)
    }
}
