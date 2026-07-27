package com.malinskiy.marathon.android.executor.listeners.screenshot

import com.malinskiy.marathon.android.StubAndroidDevice
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.DefaultTempFileFactory
import com.malinskiy.marathon.report.attachment.StubAttachmentListener
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ScreenCapturerTestRunListenerTest {
    @TempDir
    private lateinit var tempDir: File

    private val attachmentListener = StubAttachmentListener()
    private val androidDevice = StubAndroidDevice()
    private val test = stubTest()

    @Test
    fun `reports attachment when test ends`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        listener.testEnded(test, emptyMap())

        assertThat(attachmentListener.attachments).containsOnlyKeys(test)
        assertThat(attachmentListener.attachments[test]).hasSize(1)
    }

    @Test
    fun `does not report attachment for ignored test`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        listener.testIgnored(test)
        listener.testEnded(test, emptyMap())

        assertThat(attachmentListener.attachments).isEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.createListener() =
        ScreenCapturerTestRunListener(
            attachmentManager = AttachmentManager(tempDir, DefaultTempFileFactory(tempDir)),
            device = androidDevice,
            coroutineScope = this,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        ).also { it.registerListener(attachmentListener) }
}
