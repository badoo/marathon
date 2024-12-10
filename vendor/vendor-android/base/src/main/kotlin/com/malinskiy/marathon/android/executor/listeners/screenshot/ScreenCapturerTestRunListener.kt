package com.malinskiy.marathon.android.executor.listeners.screenshot

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.attachment.AttachmentListener
import com.malinskiy.marathon.report.attachment.AttachmentProvider
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async

class ScreenCapturerTestRunListener(
    private val attachmentManager: AttachmentManager,
    private val device: AndroidDevice,
    private val coroutineScope: CoroutineScope
) : TestRunListener, AttachmentProvider {

    private val attachmentListeners = mutableListOf<AttachmentListener>()
    private var screenCapturerJob: Job? = null
    private var screenCapturer: ScreenCapturer? = null
    private val logger = MarathonLogging.logger(ScreenCapturerTestRunListener::class.java.simpleName)
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    override fun registerListener(listener: AttachmentListener) {
        attachmentListeners.add(listener)
    }

    override fun testStarted(test: Test) {
        logger.debug { "Starting recording for ${test.toSimpleSafeTestName()}" }
        screenCapturer = ScreenCapturer(device, attachmentManager, test)
        screenCapturerJob = coroutineScope.async(dispatcher) {
            screenCapturer?.start()
        }
    }

    override fun testEnded(test: Test, testMetrics: Map<String, String>) {
        logger.debug { "Finished recording for ${test.toSimpleSafeTestName()}" }
        screenCapturerJob?.cancel()

        screenCapturer?.attachment?.let { attachment ->
            attachmentListeners.forEach {
                it.onAttachment(test, attachment)
            }
        }
    }
}
