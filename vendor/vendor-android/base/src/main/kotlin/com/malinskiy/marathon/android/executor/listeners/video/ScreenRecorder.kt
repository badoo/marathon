package com.malinskiy.marathon.android.executor.listeners.video

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.log.MarathonLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.system.measureTimeMillis
import kotlin.time.Duration

internal class ScreenRecorder(
    private val device: AndroidDevice,
    private val remoteFilePath: String,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val logger = MarathonLogging.getLogger(ScreenRecorder::class.java)
    private val handler = ScreenRecorderHandler()
    private var job: Job? = null

    val isCancelled: Boolean
        get() = job?.isCancelled == true

    fun start(scope: CoroutineScope) {
        job = scope.launch(ioDispatcher + CoroutineName("screen-recorder-${device.serialNumber}")) {
            try {
                // Cancellation interrupts the blocked thread, which ddmlib's poll loop observes within ~25ms
                runInterruptible { startRecordingTestVideo() }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                currentCoroutineContext().ensureActive()
                logger.error("[{}] Something went wrong while screen recording", device.serialNumber, e)
            }
        }
    }

    fun stop() {
        handler.stop()
    }

    fun await(duration: Duration) {
        runBlocking {
            withTimeoutOrNull(duration) {
                job?.join()
            }
        }
    }

    private fun startRecordingTestVideo() {
        val millis = measureTimeMillis {
            device.safeStartScreenRecorder(
                handler = handler,
                remoteFilePath = remoteFilePath,
                options = options
            )
        }
        logger.trace("[{}] Recording finished in {}ms {}", device.serialNumber, millis, remoteFilePath)
    }

    companion object {
        private const val DURATION = 180
        private const val BITRATE_MB_PER_SECOND = 1
        private val options = ScreenRecorderOptions(
            width = 0,
            height = 0,
            bitrateMbps = BITRATE_MB_PER_SECOND,
            timeLimit = DURATION.toLong(),
            timeLimitUnits = SECONDS,
            showTouches = false
        )
    }
}
