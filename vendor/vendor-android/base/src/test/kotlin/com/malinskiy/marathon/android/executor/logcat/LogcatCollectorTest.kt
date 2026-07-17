package com.malinskiy.marathon.android.executor.logcat

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.logcat.model.LogLevel
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.BatchFinished
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.BatchStarted
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.FatalError
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.Message
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.TestFinished
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.TestStarted
import com.malinskiy.marathon.android.executor.logcat.model.LogcatMessage
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.io.File
import java.time.Instant

class LogcatCollectorTest {

    @TempDir
    private lateinit var tempDir: File

    private val device = mock<AndroidDevice>()
    private val collector = LogcatCollector { prefix, extension -> File.createTempFile(prefix, extension, tempDir) }

    @Test
    fun `on test run with one batch and one test - reports one test in this batch`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertNotNull(report.batches.getValue("abc").tests[test])
    }

    @Test
    fun `on test run with one batch and one test - returns batch report by id`() = runTest {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getBatchReport("abc")

        assertNotNull(report)
        assertNotNull(report!!.tests[test])
    }

    @Test
    fun `on test run with one batch and one test with fatal error event in the same process - saves crash event for the test`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 123, device = device))
        collector.onLogcatEvent(FatalError(message = "failure", processId = 123, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 123, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertNotNull(report.batches.getValue("abc").tests[test])
        assertEquals(1, report.batches.getValue("abc").tests.getValue(test).events.size)
        assertEquals(LogEvent.Crash(message = "failure"), report.batches.getValue("abc").tests.getValue(test).events.first())
    }

    @Test
    fun `on test run with one batch and one test with fatal error event in different process - does not save crash event for the test`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 123, device = device))
        collector.onLogcatEvent(FatalError(message = "failure", processId = 9999, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 123, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertNotNull(report.batches.getValue("abc").tests[test])
        assertEquals(0, report.batches.getValue("abc").tests.getValue(test).events.size)
    }

    @Test
    fun `on test run with one batch and without test events, fatal error happened - saves crash event for the batch`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 123, device = device))
        collector.onLogcatEvent(FatalError(message = "failure", processId = 123, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 123, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertNotNull(report.batches.getValue("abc").tests[test])
        assertEquals(1, report.batches.getValue("abc").tests.getValue(test).events.size)
        assertEquals(LogEvent.Crash(message = "failure"), report.batches.getValue("abc").tests.getValue(test).events.first())
    }

    @Test
    fun `on test run with one batch and multiple tests - reports multiple tests in this batch`() {
        val test1 = LogTest("com.app", "Test1", "method")
        val test2 = LogTest("com.app", "Test2", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test1, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test1, processId = 1, device = device))
        collector.onLogcatEvent(TestStarted(test2, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test2, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertNotNull(report.batches.getValue("abc").tests[test1])
        assertNotNull(report.batches.getValue("abc").tests[test2])
    }

    @Test
    fun `on test run with one batch and one test - saves logcat messages for test to file`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertNotNull(report.batches.getValue("abc").tests[test])
        assertTrue(report.batches.getValue("abc").tests.getValue(test).file.readText().contains(".* 0-0/test E/test: Exception!\n".toRegex()))
    }

    @Test
    fun `on test run with one batch and one test and message outside of a test - includes message only for test to file`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(Message(logcatMessage = createLogcatMessage(body = "another message"), device = device))
        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertNotNull(report.batches.getValue("abc").tests[test])
        assertTrue(report.batches.getValue("abc").tests.getValue(test).file.readText().contains(".* 0-0/test E/test: Exception!\n".toRegex()))
    }

    @Test
    fun `on test run with one batch and test is not finished - saves logcat messages for test to file`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertEquals(1, report.batches.size)
        assertNotNull(report.batches["abc"])
        assertTrue(report.batches.getValue("abc").log.file.readText().contains(".* 0-0/test E/test: Exception!\n".toRegex()))
    }

    @Test
    fun `on saving messages - creates log files via the injected factory`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()

        assertThat(report.batches.getValue("abc").log.file.parentFile).isEqualTo(tempDir)
        assertThat(report.batches.getValue("abc").tests.getValue(test).file.parentFile).isEqualTo(tempDir)
    }

    @Test
    fun `multiple test runs with one batch and one test - reports logs separately`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc1", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc1", device = device))

        collector.onLogcatEvent(BatchStarted(batchId = "abc2", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc2", device = device))

        val report = collector.getFullReport()
        assertEquals(2, report.batches.size)
        assertNotNull(report.batches["abc1"])
        assertNotNull(report.batches["abc2"])
        assertNotNull(report.batches.getValue("abc1").tests[test])
        assertNotNull(report.batches.getValue("abc2").tests[test])
    }

    @Test
    fun `parallel test runs from different devices - reports logs separately`() {
        val device1 = mock<AndroidDevice>()
        val device2 = mock<AndroidDevice>()
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = createLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc1", device = device1))
        collector.onLogcatEvent(BatchStarted(batchId = "abc2", device = device2))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device2))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device2))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device2))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device1))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device1))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device1))
        collector.onLogcatEvent(BatchFinished(batchId = "abc2", device = device2))
        collector.onLogcatEvent(BatchFinished(batchId = "abc1", device = device1))

        val report = collector.getFullReport()
        assertEquals(2, report.batches.size)
        assertNotNull(report.batches["abc1"])
        assertNotNull(report.batches["abc2"])
        assertNotNull(report.batches.getValue("abc1").tests[test])
        assertNotNull(report.batches.getValue("abc2").tests[test])
    }

    private fun createLogcatMessage(
        timestamp: Instant = Instant.ofEpochMilli(1585850200000L),
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
