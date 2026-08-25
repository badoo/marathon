package com.malinskiy.marathon.android.executor.listeners.screenshot

import com.malinskiy.marathon.android.StubAndroidDevice
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.DefaultTempFileFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.TimeoutException
import javax.imageio.ImageIO

class ScreenCapturerTest {
    @TempDir
    private lateinit var tempDir: File

    private val attachmentManager by lazy {
        AttachmentManager(tempDir, DefaultTempFileFactory(tempDir))
    }
    private val androidDevice = StubAndroidDevice()

    @Test
    fun `does not publish attachment when no frame was captured`() = runTest {
        androidDevice.screenshotProvider = { throw TimeoutException() }
        val capturer = createCapturer()

        capturer.start(this)
        capturer.close()

        assertThat(capturer.attachment).isNull()
    }

    @Test
    fun `publishes valid gif attachment once a frame was written`() = runTest {
        val capturer = createCapturer()

        capturer.start(this)
        capturer.close()

        assertThat(capturer.attachment).isNotNull
        val gifBytes = capturer.attachment?.file?.readBytes() ?: ByteArray(0)
        assertThat(gifBytes).startsWith(*GIF_HEADER.toByteArray(Charsets.US_ASCII))
        assertThat(gifBytes).endsWith(GIF_TRAILER)
    }

    @Test
    fun `keeps the device orientation of captured frames`() = runTest {
        androidDevice.screenshotProvider = { BufferedImage(720, 1280, BufferedImage.TYPE_INT_ARGB) }
        val capturer = createCapturer()

        capturer.start(this)
        capturer.close()

        val frame = ImageIO.read(capturer.attachment?.file)
        assertThat(frame.width).isEqualTo(720)
        assertThat(frame.height).isEqualTo(1280)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.createCapturer() = ScreenCapturer(
        attachmentManager = attachmentManager,
        device = androidDevice,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private companion object {
        private const val GIF_HEADER = "GIF89a"
        private const val GIF_TRAILER = 0x3B.toByte()
    }
}
