package com.malinskiy.marathon.android.executor.logcat.parse

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.logcat.model.LogLevel
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.DeviceDisconnected
import com.malinskiy.marathon.android.executor.logcat.model.LogcatMessage
import com.malinskiy.marathon.report.logs.LogTest
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldEqualTo
import org.junit.jupiter.api.Test
import java.time.Instant

class LogcatEventsAdapterTest {

    private val output = mutableListOf<LogcatEvent>()
    private val adapter = LogcatEventsAdapter(object : LogcatEventsListener {
        override fun onLogcatEvent(event: LogcatEvent) {
            output.add(event)
        }
    })

    @Test
    fun `on message with device - creates logcat event with the same device`() {
        val device = mock<AndroidDevice>()

        adapter.onMessage(device, createLogcatMessage())

        output.size shouldEqualTo 1
        output.first().device shouldBe device
    }

    @Test
    fun `on device disconnected - passes disconnected event`() {
        val device = mock<AndroidDevice>()

        adapter.onDeviceDisconnected(device)

        output.size shouldEqualTo 1
        output.first() shouldBeInstanceOf DeviceDisconnected::class.java
        output.first().device shouldBe device
    }

    @Test
    fun `on test start logcat message - passes test started event and logcat message event`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "TestRunner",
            body = "started: testMethod(com.test.app.TestClass)"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.TestStarted(
                test = LogTest("com.test.app", "TestClass", "testMethod"),
                processId = 123,
                device = device
            ),
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            )
        )
    }

    @Test
    fun `on sigsegv crash logcat message - passes fatal error crash event and logcat message event`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "libc",
            body = "Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0 in tid 14689 (Firebase-Fireba), pid 14554 (com.example.app)"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            ),
            LogcatEvent.FatalError(
                message = "Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x0 in tid 14689 (Firebase-Fireba), pid 14554 (com.example.app)",
                processId = 123,
                device = device
            )
        )
    }

    @Test
    fun `on zygote fatal signal 11 logcat message - passes fatal error crash event and logcat message event`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "Zygote",
            body = "Process 6755 exited due to signal (11)"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            ),
            LogcatEvent.FatalError(
                message = "Process 6755 exited due to signal (11)",
                processId = 6755,
                device = device
            )
        )
    }

    @Test
    fun `on zygote fatal signal 9 logcat message - ignores crash event and passes logcat message event`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "Zygote",
            body = "Process 6755 exited due to signal 9"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            )
        )
    }

    @Test
    fun `on zygote not process exit logcat message - passes only logcat message itself`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "Zygote",
            body = "Not late-enabling -Xcheck:jni (already on)"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            )
        )
    }

    @Test
    fun `on android jvm crash logcat message - passes fatal error event and logcat message event`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "AndroidRuntime",
            body = """FATAL EXCEPTION: main
	Process: com.example.app, PID: 15943
	java.lang.IllegalStateException: TestException"""
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            ),
            LogcatEvent.FatalError(
                message = """FATAL EXCEPTION: main
	Process: com.example.app, PID: 15943
	java.lang.IllegalStateException: TestException""",
                processId = 123,
                device = device
            )
        )
    }

    @Test
    fun `on test finished logcat message - passes test finished event and logcat message event`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "TestRunner",
            body = "finished: testMethod(com.test.app.TestClass)"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            ),
            LogcatEvent.TestFinished(
                test = LogTest("com.test.app", "TestClass", "testMethod"),
                processId = 123,
                device = device
            )
        )
    }

    @Test
    fun `on test start logcat message - no package name - passes test started event with empty package name`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            processId = 123,
            tag = "TestRunner",
            body = "started: testMethod(TestClass)"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.TestStarted(
                test = LogTest("", "TestClass", "testMethod"),
                processId = 123,
                device = device
            ),
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            )
        )
    }

    @Test
    fun `on batch start logcat message - passes batch started event with batch id`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            tag = "marathon",
            body = "batch_started: {abcdef}"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.BatchStarted(
                batchId = "abcdef",
                device = device
            ),
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            )
        )
    }

    @Test
    fun `on batch finished logcat message - passes batch finished event with batch id`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            tag = "marathon",
            body = "batch_finished: {abcdef}"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            ),
            LogcatEvent.BatchFinished(
                batchId = "abcdef",
                device = device
            )
        )
    }

    @Test
    fun `on unknown logcat message - passes generic logcat message event`() {
        val device = mock<AndroidDevice>()
        val message = createLogcatMessage(
            tag = "TestLog",
            body = "some log"
        )

        adapter.onMessage(device, message)

        output shouldEqual listOf(
            LogcatEvent.Message(
                logcatMessage = message,
                device = device
            )
        )
    }

    private fun createLogcatMessage(
        timestamp: Instant = Instant.now(),
        processId: Int = 0,
        threadId: Int = 0,
        applicationName: String = "test",
        logLevel: LogLevel = LogLevel.ERROR,
        tag: String = "test",
        body: String = "test"
    ): LogcatMessage = LogcatMessage(
        timestamp = timestamp,
        processId = processId,
        threadId = threadId,
        applicationName = applicationName,
        logLevel = logLevel,
        tag = tag,
        body = body
    )
}
