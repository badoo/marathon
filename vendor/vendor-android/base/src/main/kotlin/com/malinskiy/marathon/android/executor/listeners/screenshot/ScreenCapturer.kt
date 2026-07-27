package com.malinskiy.marathon.android.executor.listeners.screenshot

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.exception.CommandRejectedException
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.log.MarathonLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.imgscalr.Scalr
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.awt.image.RenderedImage
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.imageio.stream.FileImageOutputStream
import kotlin.system.measureTimeMillis

internal class ScreenCapturer(
    private val attachmentManager: AttachmentManager,
    private val device: AndroidDevice,
    private val ioDispatcher: CoroutineDispatcher
) : AutoCloseable {

    private val logger = MarathonLogging.getLogger(ScreenCapturer::class.java)
    private var job: Job? = null

    var attachment: Attachment? = null
        private set

    fun start(scope: CoroutineScope) {
        job = scope.launch(ioDispatcher + CoroutineName("screen-capturer-${device.serialNumber}")) {
            try {
                capture()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                currentCoroutineContext().ensureActive()
                logger.error("[{}] Error while capturing screenshots", device.serialNumber, e)
            }
        }
    }

    override fun close() {
        runBlocking { job?.cancelAndJoin() }
    }

    private suspend fun capture() = coroutineScope {
        val attachment = attachmentManager.createAttachment(FileType.SCREENSHOT, AttachmentType.SCREENSHOT)
        FileImageOutputStream(attachment.file).use { outputStream ->
            GifSequenceWriter(outputStream, TYPE_INT_ARGB, DELAY, true).use { writer ->
                var targetOrientation = UNDEFINED
                while (isActive) {
                    val capturingTimeMillis = measureTimeMillis {
                        getScreenshot(targetOrientation)?.let {
                            if (targetOrientation == UNDEFINED) {
                                // remember the target orientation
                                targetOrientation = it.getOrientation()
                            }
                            writer.writeToSequence(it)
                            this@ScreenCapturer.attachment = attachment
                        }
                    }
                    val sleepTimeMillis = when {
                        (DELAY - capturingTimeMillis) < 0 -> 0
                        else -> DELAY - capturingTimeMillis
                    }
                    delay(sleepTimeMillis)
                }
            }
        }
    }

    private fun getScreenshot(targetOrientation: Int): RenderedImage? {
        return try {
            val screenshot = device.getScreenshot(TIMEOUT_MS, TimeUnit.MILLISECONDS).let {
                // in case the orientation of the image is different than the target, rotate by 90 degrees
                if (targetOrientation != UNDEFINED && it.getOrientation() != targetOrientation) {
                    Scalr.rotate(it, Scalr.Rotation.CW_90).also { org -> org.flush() }
                } else {
                    it
                }
            }

            // the first time the orientation did not settle, use the actual image orientation
            val resolvedOrientation = if (targetOrientation == UNDEFINED) screenshot.getOrientation() else targetOrientation
            if (resolvedOrientation == PORTRAIT) {
                Scalr.resize(screenshot, Scalr.Method.SPEED, Scalr.Mode.AUTOMATIC, TARGET_WIDTH, TARGET_HEIGHT)
            } else {
                Scalr.resize(screenshot, Scalr.Method.SPEED, Scalr.Mode.AUTOMATIC, TARGET_HEIGHT, TARGET_WIDTH)
            }
        } catch (e: TimeoutException) {
            logger.error("Timeout. Exiting", e)
            null
        } catch (e: IOException) {
            logger.error("Screenshot capture failed", e)
            null
        } catch (e: CommandRejectedException) {
            logger.error("ADB is not responding. Exiting", e)
            null
        }
    }

    /** retrieves the orientation of the RenderImage */
    private fun RenderedImage.getOrientation(): Int =
        if (width > height) LANDSCAPE else PORTRAIT

    companion object {
        const val DELAY = 500
        const val TIMEOUT_MS = 300L

        private const val TARGET_WIDTH = 720
        private const val TARGET_HEIGHT = 1280
        private const val UNDEFINED = 0
        private const val PORTRAIT = 1
        private const val LANDSCAPE = 2
    }
}
