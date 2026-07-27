package com.malinskiy.marathon.android.executor.listeners.screenshot

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.attachment.AttachmentListener
import com.malinskiy.marathon.report.attachment.AttachmentProvider
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

class ScreenCapturerTestRunListener(
    private val attachmentManager: AttachmentManager,
    private val device: AndroidDevice,
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher
) : TestRunListener, AttachmentProvider {

    private val attachmentListeners = mutableListOf<AttachmentListener>()
    private var screenCapturer: ScreenCapturer? = null
    private val logger = MarathonLogging.getLogger(ScreenCapturerTestRunListener::class.java)

    override fun registerListener(listener: AttachmentListener) {
        attachmentListeners.add(listener)
    }

    override fun testStarted(test: Test) {
        logger.debug("Starting recording for test {}", test.toSimpleSafeTestName())
        screenCapturer?.close()
        screenCapturer = ScreenCapturer(attachmentManager, device, ioDispatcher).apply { start(coroutineScope) }
    }

    override fun testIgnored(test: Test) {
        screenCapturer?.close()
        screenCapturer = null
    }

    override fun testEnded(test: Test, testMetrics: Map<String, String>) {
        logger.debug("Finished recording for test {}", test.toSimpleSafeTestName())
        screenCapturer?.close()

        screenCapturer?.attachment?.let { attachment ->
            attachmentListeners.forEach {
                it.onAttachment(test, attachment)
            }
        }
    }

    override fun testRunFailed(errorMessage: String) {
        screenCapturer?.close()
    }

    override fun testRunStopped(elapsedTime: Long) {
        screenCapturer?.close()
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        screenCapturer?.close()
    }
}
