package com.malinskiy.marathon.android.executor.listeners.video

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.exception.TransferException
import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.attachment.AttachmentListener
import com.malinskiy.marathon.report.attachment.AttachmentProvider
import com.malinskiy.marathon.test.Test
import kotlin.system.measureTimeMillis

const val MS_IN_SECOND: Long = 1_000L

class ScreenRecorderTestRunListener(
    private val attachmentManager: AttachmentManager,
    private val device: AndroidDevice
) : TestRunListener, AttachmentProvider {

    private val attachmentListeners = mutableListOf<AttachmentListener>()
    private val logger = MarathonLogging.getLogger(ScreenRecorderTestRunListener::class.java)

    private var handler: ScreenRecorderHandler? = null
    private val screenRecorderStopper = ScreenRecorderStopper(device)

    private var hasFailed: Boolean = false
    private var recorder: Thread? = null

    private val awaitMillis = MS_IN_SECOND

    override fun registerListener(listener: AttachmentListener) {
        attachmentListeners.add(listener)
    }

    override fun testStarted(test: Test) {
        hasFailed = false

        val screenRecorder = ScreenRecorder(device, device.fileManager.remoteVideoForTest(test))
        handler = ScreenRecorderHandler()
        recorder = kotlin.concurrent.thread {
            screenRecorder.run(checkNotNull(handler))
        }
    }

    override fun testFailed(test: Test, trace: String) {
        hasFailed = true
    }

    override fun testAssumptionFailure(test: Test, trace: String) {
        handler?.stop()
        pullVideo(test)
    }

    override fun testEnded(test: Test, testMetrics: Map<String, String>) {
        handler?.stop()
        pullVideo(test)
    }

    private fun pullVideo(test: Test) {
        try {
            val joinMillis = measureTimeMillis {
                recorder?.join(awaitMillis)
            }
            logger.trace("[{}] Awaited screen recording in {}ms", device.serialNumber, joinMillis)
            if (hasFailed) {
                val stopMillis = measureTimeMillis {
                    screenRecorderStopper.stopScreenRecord()
                }
                logger.trace("[{}] Stopped screen recording in {}ms", device.serialNumber, stopMillis)
                pullTestVideo(test)
            }
            removeTestVideo(test)
        } catch (e: InterruptedException) {
            logger.warn("[{}] Failed to stop screen recording", device.serialNumber, e)
        } catch (e: TransferException) {
            logger.warn("[{}] Failed to pull video", device.serialNumber, e)
        }
    }

    private fun pullTestVideo(test: Test) {
        val attachment = attachmentManager.createAttachment(
            FileType.VIDEO,
            AttachmentType.VIDEO
        )
        val localVideoFile = attachment.file
        val remoteFilePath = device.fileManager.remoteVideoForTest(test)
        val millis = measureTimeMillis {
            device.fileManager.pullFile(remoteFilePath, localVideoFile)
        }
        logger.trace("[{}] Pulling video finished in {}ms {}", device.serialNumber, millis, remoteFilePath)
        attachmentListeners.forEach { it.onAttachment(test, attachment) }
    }

    private fun removeTestVideo(test: Test) {
        val remoteFilePath = device.fileManager.remoteVideoForTest(test)
        val millis = measureTimeMillis {
            device.fileManager.remove(remoteFilePath)
        }
        logger.trace("[{}] Removed file in {}ms {}", device.serialNumber, millis, remoteFilePath)
    }
}
