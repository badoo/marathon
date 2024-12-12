package com.malinskiy.marathon.android.executor.listeners.video

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.log.MarathonLogging
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.system.measureTimeMillis

internal class ScreenRecorder(
    private val device: AndroidDevice,
    private val remoteFilePath: String
) {
    private val logger = MarathonLogging.getLogger(ScreenRecorder::class.java)

    fun run(handler: ScreenRecorderHandler) {
        return try {
            startRecordingTestVideo(handler)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.error("[{}] Something went wrong while screen recording", device.serialNumber, e)
        }
    }

    private fun startRecordingTestVideo(handler: ScreenRecorderHandler) {
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
            0,
            0,
            BITRATE_MB_PER_SECOND,
            timeLimit = DURATION.toLong(),
            timeLimitUnits = SECONDS,
            showTouches = false
        )
    }
}
