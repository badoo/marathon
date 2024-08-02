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

    override fun registerListener(listener: AttachmentListener) {
        attachmentListeners.add(listener)
    }

    private val logger = MarathonLogging.logger("ScreenRecorder")

    private var handler: ScreenRecorderHandler? = null
    private val screenRecorderStopper = ScreenRecorderStopper(device)

    private var hasFailed: Boolean = false
    private var recorder: Thread? = null

    private val awaitMillis = MS_IN_SECOND

    override fun testStarted(test: Test) {
        hasFailed = false

        val screenRecorder = ScreenRecorder(device, device.fileManager.remoteVideoForTest(test))
        handler = ScreenRecorderHandler()
        recorder = kotlin.concurrent.thread {
            screenRecorder.run(handler!!)
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
            val join = measureTimeMillis {
                recorder?.join(awaitMillis)
            }
            logger.trace { "join ${join}ms" }
            if (hasFailed) {
                val stop = measureTimeMillis {
                    screenRecorderStopper.stopScreenRecord()
                }
                logger.trace { "stop ${stop}ms" }
                pullTestVideo(test)
            }
            removeTestVideo(test)
        } catch (e: InterruptedException) {
            logger.warn("Can't stop recording", e)
        } catch (e: TransferException) {
            logger.warn("Can't pull video", e)
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
        logger.trace { "Pulling finished in ${millis}ms $remoteFilePath " }
        attachmentListeners.forEach { it.onAttachment(test, attachment) }
    }

    private fun removeTestVideo(test: Test) {
        val remoteFilePath = device.fileManager.remoteVideoForTest(test)
        val millis = measureTimeMillis {
            device.fileManager.remove(remoteFilePath)
        }
        logger.trace { "Removed file in ${millis}ms $remoteFilePath" }
    }
}
