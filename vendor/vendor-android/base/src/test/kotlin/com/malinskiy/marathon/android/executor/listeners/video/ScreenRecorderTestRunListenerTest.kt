package com.malinskiy.marathon.android.executor.listeners.video

import com.malinskiy.marathon.android.StubAndroidDevice
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.DefaultTempFileFactory
import com.malinskiy.marathon.report.attachment.StubAttachmentListener
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Duration

class ScreenRecorderTestRunListenerTest {
    @TempDir
    private lateinit var tempDir: File

    private val test = stubTest()
    private val attachmentListener = StubAttachmentListener()
    private val androidDevice = StubAndroidDevice()

    @Test
    fun `removes remote video without pulling when test passes`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        listener.testEnded(test, emptyMap())

        assertThat(recording.isFinished).isTrue()
        assertThat(attachmentListener.attachments).isEmpty()
        assertThat(androidDevice.pulledFiles).isEmpty()
        assertThat(androidDevice.executedCommands).containsExactly("rm -r ${androidDevice.fileManager.remoteVideoForTest(test)}")
    }

    @Test
    fun `pulls video when test fails`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        listener.testFailed(test, "trace")
        listener.testEnded(test, emptyMap())

        assertThat(recording.isFinished).isTrue()
        assertThat(attachmentListener.attachments).containsOnlyKeys(test)
        assertThat(attachmentListener.attachments[test]).extracting("type").containsExactly(AttachmentType.VIDEO)
        assertThat(androidDevice.pulledFiles).containsExactly(androidDevice.fileManager.remoteVideoForTest(test))
    }

    @Test
    fun `does not report attachment when the pulled video is empty`() = runTest {
        androidDevice.pulledFileContent = ByteArray(0)
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        listener.testFailed(test, "trace")
        listener.testEnded(test, emptyMap())

        assertThat(recording.isFinished).isTrue()
        assertThat(attachmentListener.attachments).isEmpty()
        assertThat(androidDevice.pulledFiles).containsExactly(androidDevice.fileManager.remoteVideoForTest(test))
        assertThat(androidDevice.executedCommands).containsExactly("rm -r ${androidDevice.fileManager.remoteVideoForTest(test)}")
    }

    @Test
    fun `stops straggler recording when the next test starts`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val stragglerRecording = androidDevice.awaitScreenRecording()
        listener.testStarted(stubTest(method = "nextTest"))
        val finished = stragglerRecording.awaitFinished(AWAIT_TIMEOUT)

        assertThat(finished).isTrue()
    }

    @Test
    fun `stops recording when the test run ends`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        listener.testRunEnded(0, emptyMap())
        val finished = recording.awaitFinished(AWAIT_TIMEOUT)

        assertThat(finished).isTrue()
    }

    @Test
    fun `stops recording when the test run fails`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        listener.testRunFailed("run error")
        val finished = recording.awaitFinished(AWAIT_TIMEOUT)

        assertThat(finished).isTrue()
    }

    @Test
    fun `stops recording when the test run is stopped`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        listener.testRunStopped(0)
        val finished = recording.awaitFinished(AWAIT_TIMEOUT)

        assertThat(finished).isTrue()
    }

    @Test
    fun `stops recording when the device scope is cancelled`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        backgroundScope.cancel()
        val finished = recording.awaitFinished(AWAIT_TIMEOUT)

        assertThat(finished).isTrue()
    }

    @Test
    fun `skips video pulling when recording was cancelled`() = runTest {
        val listener = createListener()

        listener.testStarted(test)
        val recording = androidDevice.awaitScreenRecording()
        listener.testFailed(test, "trace")
        backgroundScope.cancel()
        val finished = recording.awaitFinished(AWAIT_TIMEOUT)
        listener.testEnded(test, emptyMap())

        assertThat(finished).isTrue()
        assertThat(attachmentListener.attachments).isEmpty()
        assertThat(androidDevice.pulledFiles).isEmpty()
        assertThat(androidDevice.executedCommands).isEmpty()
        assertThat(androidDevice.executedShellCommands).isEmpty()
    }

    @Test
    fun `does not start recording after the device scope is cancelled`() = runTest {
        val listener = createListener()

        backgroundScope.cancel()
        listener.testStarted(test)
        listener.testEnded(test, emptyMap())

        assertThat(androidDevice.screenRecordings).isEmpty()
        assertThat(androidDevice.executedCommands).isEmpty()
    }

    private fun TestScope.createListener() =
        ScreenRecorderTestRunListener(
            attachmentManager = AttachmentManager(tempDir, DefaultTempFileFactory(tempDir)),
            device = androidDevice,
            coroutineScope = backgroundScope,
            ioDispatcher = Dispatchers.IO
        ).also { it.registerListener(attachmentListener) }

    private companion object {
        private val AWAIT_TIMEOUT = Duration.ofSeconds(5)
    }
}
