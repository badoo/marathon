package com.malinskiy.marathon.android.executor.logcat

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.BatchFinished
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.BatchStarted
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.FatalError
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.Message
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.TestFinished
import com.malinskiy.marathon.android.executor.logcat.model.LogcatEvent.TestStarted
import com.malinskiy.marathon.android.executor.logcat.model.stubLogcatMessage
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.io.File

class LogcatCollectorTest {

    @TempDir
    private lateinit var tempDir: File

    private val device = mock<AndroidDevice>()
    private val collector = LogcatCollector { prefix, extension -> File.createTempFile(prefix, extension, tempDir) }

    @Test
    fun `on test run with one batch and one test - reports one test in this batch`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").tests).containsKey(test)
    }

    @Test
    fun `on test run with one batch and one test - returns batch report by id`() = runTest {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getBatchReport("abc")

        assertThat(report).isNotNull()
        assertThat(report!!.tests).containsKey(test)
    }

    @Test
    fun `on test run with one batch and one test with fatal error event in the same process - saves crash event for the test`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 123, device = device))
        collector.onLogcatEvent(FatalError(message = "failure", processId = 123, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 123, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").tests).containsKey(test)
        assertThat(report.batches.getValue("abc").tests.getValue(test).events).containsExactly(LogEvent.Crash(message = "failure"))
    }

    @Test
    fun `on test run with one batch and one test with fatal error event in different process - does not save crash event for the test`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 123, device = device))
        collector.onLogcatEvent(FatalError(message = "failure", processId = 9999, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 123, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").tests).containsKey(test)
        assertThat(report.batches.getValue("abc").tests.getValue(test).events).isEmpty()
    }

    @Test
    fun `on test run with one batch and without test events, fatal error happened - saves crash event for the batch`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 123, device = device))
        collector.onLogcatEvent(FatalError(message = "failure", processId = 123, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 123, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").tests).containsKey(test)
        assertThat(report.batches.getValue("abc").tests.getValue(test).events).containsExactly(LogEvent.Crash(message = "failure"))
    }

    @Test
    fun `on test run with one batch and multiple tests - reports multiple tests in this batch`() {
        val test1 = LogTest("com.app", "Test1", "method")
        val test2 = LogTest("com.app", "Test2", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test1, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test1, processId = 1, device = device))
        collector.onLogcatEvent(TestStarted(test2, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test2, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").tests).containsKeys(test1, test2)
    }

    @Test
    fun `on test run with one batch and one test - saves logcat messages for test to file`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").tests).containsKey(test)
        assertThat(report.batches.getValue("abc").tests.getValue(test).file.readText()).containsPattern(".* 0-0/test E/test: Exception!\n")
    }

    @Test
    fun `on test run with one batch and one test and message outside of a test - includes message only for test to file`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(Message(logcatMessage = stubLogcatMessage(body = "another message"), device = device))
        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(TestFinished(test, processId = 1, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").tests).containsKey(test)
        assertThat(report.batches.getValue("abc").tests.getValue(test).file.readText()).containsPattern(".* 0-0/test E/test: Exception!\n")
    }

    @Test
    fun `on test run with one batch and test is not finished - saves logcat messages for test to file`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

        collector.onLogcatEvent(BatchStarted(batchId = "abc", device = device))
        collector.onLogcatEvent(TestStarted(test, processId = 1, device = device))
        collector.onLogcatEvent(Message(logcatMessage = logcatMessage, device = device))
        collector.onLogcatEvent(BatchFinished(batchId = "abc", device = device))

        val report = collector.getFullReport()
        assertThat(report.batches).containsOnlyKeys("abc")
        assertThat(report.batches.getValue("abc").log.file.readText()).containsPattern(".* 0-0/test E/test: Exception!\n")
    }

    @Test
    fun `on saving messages - creates log files via the injected factory`() {
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

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
        val logcatMessage = stubLogcatMessage(body = "Exception!")

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
        assertThat(report.batches).containsOnlyKeys("abc1", "abc2")
        assertThat(report.batches.getValue("abc1").tests).containsKey(test)
        assertThat(report.batches.getValue("abc2").tests).containsKey(test)
    }

    @Test
    fun `parallel test runs from different devices - reports logs separately`() {
        val device1 = mock<AndroidDevice>()
        val device2 = mock<AndroidDevice>()
        val test = LogTest("com.app", "Test", "method")
        val logcatMessage = stubLogcatMessage(body = "Exception!")

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
        assertThat(report.batches).containsOnlyKeys("abc1", "abc2")
        assertThat(report.batches.getValue("abc1").tests).containsKey(test)
        assertThat(report.batches.getValue("abc2").tests).containsKey(test)
    }
}
