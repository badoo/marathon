package com.malinskiy.marathon.android.executor.listeners

import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.NetworkState
import com.malinskiy.marathon.device.OperatingSystem
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.attachment.AttachmentListener
import com.malinskiy.marathon.report.attachment.AttachmentProvider
import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.test.stubTestBatch
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import com.malinskiy.marathon.test.Test as MarathonTest

class TestRunResultsListenerTest {
    private val test1 = stubTest(method = "test1")
    private val test2 = stubTest(method = "test2")
    private val poolId = DevicePoolId("pool")
    private val device = mock<Device> {
        on { operatingSystem } doReturn OperatingSystem("29")
        on { serialNumber } doReturn "serial-1"
        on { model } doReturn "model"
        on { manufacturer } doReturn "manufacturer"
        on { networkState } doReturn NetworkState.CONNECTED
        on { deviceFeatures } doReturn emptyList()
        on { healthy } doReturn true
    }
    private val timer = mock<Timer>()
    private val progressReporter = mock<ProgressReporter>()
    private val strictRunChecker = mock<StrictRunChecker>()
    private val deferred = CompletableDeferred<TestBatchResults>()

    @Test
    fun `reports passed tests as finished`() = runTest {
        val batch = stubTestBatch(test1, test2)
        val listener = createListener(batch)

        listener.testRunStarted("run", 2)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testStarted(test2)
        listener.testEnded(test2, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        assertThat(results.batchId).isEqualTo(batch.id)
        assertThat(results.device).isEqualTo(device)
        assertThat(results.finished).flatExtracting({ it.test }).containsExactlyInAnyOrder(test1, test2)
        assertThat(results.finished).allMatch { it.status == TestStatus.PASSED }
        assertThat(results.failed).isEmpty()
        assertThat(results.uncompleted).isEmpty()
    }

    @Test
    fun `reports failed test with its stacktrace`() = runTest {
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testFailed(test1, "trace")
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val failedResult = results.failed.single()
        assertThat(failedResult.test).isEqualTo(test1)
        assertThat(failedResult.status).isEqualTo(TestStatus.FAILURE)
        assertThat(failedResult.stacktrace).isEqualTo("trace")
        assertThat(results.finished).isEmpty()
        assertThat(results.uncompleted).isEmpty()
    }

    @Test
    fun `reports ignored test as finished`() = runTest {
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testIgnored(test1)
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test).isEqualTo(test1)
        assertThat(finishedResult.status).isEqualTo(TestStatus.IGNORED)
        assertThat(results.failed).isEmpty()
        assertThat(results.uncompleted).isEmpty()
    }

    @Test
    fun `reports assumption failure as finished`() = runTest {
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testAssumptionFailure(test1, "assumption trace")
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.status).isEqualTo(TestStatus.ASSUMPTION_FAILURE)
        assertThat(finishedResult.stacktrace).isEqualTo("assumption trace")
        assertThat(results.failed).isEmpty()
    }

    @Test
    fun `reports test started but never ended as uncompleted`() = runTest {
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testRunFailed("process crashed")

        val results = deferred.await()
        val uncompletedResult = results.uncompleted.single()
        assertThat(uncompletedResult.test).isEqualTo(test1)
        assertThat(uncompletedResult.status).isEqualTo(TestStatus.INCOMPLETE)
        assertThat(results.finished).isEmpty()
        assertThat(results.failed).isEmpty()
    }

    @Test
    fun `reports batch tests missing from the run as uncompleted with the run failure message`() = runTest {
        val listener = createListener(stubTestBatch(test1, test2))

        listener.testRunStarted("run", 2)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testRunFailed("run crashed")

        val results = deferred.await()
        val finishedResult = results.finished.single()
        val uncompletedResult = results.uncompleted.single()
        assertThat(finishedResult.test).isEqualTo(test1)
        assertThat(uncompletedResult.test).isEqualTo(test2)
        assertThat(uncompletedResult.status).isEqualTo(TestStatus.INCOMPLETE)
        assertThat(uncompletedResult.stacktrace).isEqualTo("run crashed")
        assertThat(uncompletedResult.startTime).isEqualTo(finishedResult.endTime)
    }

    @Test
    fun `uses listener creation time for uncompleted tests when nothing ran`() = runTest {
        whenever(timer.currentTimeMillis()).thenReturn(1000L, 2000L)
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testRunFailed("failed to start")

        val results = deferred.await()
        val uncompletedResult = results.uncompleted.single()
        assertThat(uncompletedResult.status).isEqualTo(TestStatus.INCOMPLETE)
        assertThat(uncompletedResult.startTime).isEqualTo(1000L)
        assertThat(uncompletedResult.endTime).isEqualTo(2000L)
        assertThat(uncompletedResult.stacktrace).isEqualTo("failed to start")
    }

    @Test
    fun `includes tests reported by the run but missing from the batch`() = runTest {
        val unexpectedTest = stubTest(method = "unexpected")
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 2)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testStarted(unexpectedTest)
        listener.testEnded(unexpectedTest, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        assertThat(results.finished).flatExtracting({ it.test }).containsExactlyInAnyOrder(test1, unexpectedTest)
        assertThat(results.uncompleted).isEmpty()
    }

    @Test
    fun `drops results with null method that are not in the batch`() = runTest {
        val beforeClassFailure = stubTest(method = "null")
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(beforeClassFailure)
        listener.testFailed(beforeClassFailure, "trace")
        listener.testEnded(beforeClassFailure, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        assertThat(results.finished).isEmpty()
        assertThat(results.failed).isEmpty()
        assertThat(results.uncompleted).flatExtracting({ it.test }).containsExactly(test1)
    }

    @Test
    fun `keeps results with null method that are explicitly in the batch`() = runTest {
        val ignoredClass = stubTest(method = "null")
        val listener = createListener(stubTestBatch(ignoredClass))

        listener.testRunStarted("run", 1)
        listener.testStarted(ignoredClass)
        listener.testIgnored(ignoredClass)
        listener.testEnded(ignoredClass, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test).isEqualTo(ignoredClass)
        assertThat(finishedResult.status).isEqualTo(TestStatus.IGNORED)
        assertThat(results.uncompleted).isEmpty()
    }

    @Test
    fun `attaches provided attachments to the matching test result`() = runTest {
        val attachment = Attachment(file = File("screenshot.gif"), type = AttachmentType.SCREENSHOT, fileType = FileType.SCREENSHOT)
        val provider = TestAttachmentProvider()
        val listener = createListener(batch = stubTestBatch(test1, test2), attachmentProviders = listOf(provider))

        provider.send(test1, attachment)
        listener.testRunStarted("run", 2)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testStarted(test2)
        listener.testEnded(test2, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val test1Result = results.finished.single { it.test == test1 }
        val test2Result = results.finished.single { it.test == test2 }
        assertThat(test1Result.attachments).containsExactly(attachment)
        assertThat(test2Result.attachments).isEmpty()
    }

    @Test
    fun `attaches provided attachments to a failed test result`() = runTest {
        val attachment = Attachment(file = File("screenshot.gif"), type = AttachmentType.SCREENSHOT, fileType = FileType.SCREENSHOT)
        val provider = TestAttachmentProvider()
        val listener = createListener(batch = stubTestBatch(test1), attachmentProviders = listOf(provider))

        provider.send(test1, attachment)
        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testFailed(test1, "trace")
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val failedResult = results.failed.single()
        assertThat(failedResult.attachments).containsExactly(attachment)
    }

    @Test
    fun `collects multiple attachments for the same test`() = runTest {
        val screenshot = Attachment(file = File("screenshot.gif"), type = AttachmentType.SCREENSHOT, fileType = FileType.SCREENSHOT)
        val log = Attachment(file = File("test.log"), type = AttachmentType.LOG, fileType = FileType.LOG)
        val provider = TestAttachmentProvider()
        val listener = createListener(batch = stubTestBatch(test1), attachmentProviders = listOf(provider))

        provider.send(test1, screenshot)
        provider.send(test1, log)
        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.attachments).containsExactly(screenshot, log)
    }

    @Test
    fun `drops attachments reported under the parameterized test name from the merged result`() = runTest {
        val attachment = Attachment(file = File("screenshot.gif"), type = AttachmentType.SCREENSHOT, fileType = FileType.SCREENSHOT)
        val parameterizedTest = stubTest(method = "parameterized")
        val provider = TestAttachmentProvider()
        val listener = createListener(batch = stubTestBatch(parameterizedTest), attachmentProviders = listOf(provider))

        provider.send(stubTest(method = "parameterized[0]"), attachment)
        listener.testRunStarted("run", 1)
        listener.testStarted(stubTest(method = "parameterized[0]"))
        listener.testEnded(stubTest(method = "parameterized[0]"), emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test).isEqualTo(parameterizedTest)
        assertThat(finishedResult.attachments).isEmpty()
    }

    @Test
    fun `merges parameterized results into a single result`() = runTest {
        val parameterizedTest = stubTest(method = "parameterized")
        val listener = createListener(stubTestBatch(parameterizedTest))

        listener.testRunStarted("run", 2)
        listener.testStarted(stubTest(method = "parameterized[0]"))
        listener.testEnded(stubTest(method = "parameterized[0]"), emptyMap())
        listener.testStarted(stubTest(method = "parameterized[1]"))
        listener.testEnded(stubTest(method = "parameterized[1]"), emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test).isEqualTo(parameterizedTest)
        assertThat(finishedResult.status).isEqualTo(TestStatus.PASSED)
        assertThat(results.uncompleted).isEmpty()
        verify(progressReporter).addTestDiscoveredDuringRuntime(poolId, stubTest(method = "parameterized[1]"))
    }

    @Test
    fun `merged parameterized result keeps the failure of an earlier parameter`() = runTest {
        val parameterizedTest = stubTest(method = "parameterized")
        val listener = createListener(stubTestBatch(parameterizedTest))

        listener.testRunStarted("run", 2)
        listener.testStarted(stubTest(method = "parameterized[0]"))
        listener.testFailed(stubTest(method = "parameterized[0]"), "trace")
        listener.testEnded(stubTest(method = "parameterized[0]"), emptyMap())
        listener.testStarted(stubTest(method = "parameterized[1]"))
        listener.testEnded(stubTest(method = "parameterized[1]"), emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val failedResult = results.failed.single()
        assertThat(failedResult.test).isEqualTo(parameterizedTest)
        assertThat(failedResult.status).isEqualTo(TestStatus.FAILURE)
        assertThat(results.finished).isEmpty()
    }

    @Test
    fun `merged parameterized result keeps ignored status even when a later parameter fails`() = runTest {
        val parameterizedTest = stubTest(method = "parameterized")
        val listener = createListener(stubTestBatch(parameterizedTest))

        listener.testRunStarted("run", 2)
        listener.testStarted(stubTest(method = "parameterized[0]"))
        listener.testIgnored(stubTest(method = "parameterized[0]"))
        listener.testEnded(stubTest(method = "parameterized[0]"), emptyMap())
        listener.testStarted(stubTest(method = "parameterized[1]"))
        listener.testFailed(stubTest(method = "parameterized[1]"), "trace")
        listener.testEnded(stubTest(method = "parameterized[1]"), emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test).isEqualTo(parameterizedTest)
        assertThat(finishedResult.status).isEqualTo(TestStatus.IGNORED)
        assertThat(results.failed).isEmpty()
    }

    @Test
    fun `merged parameterized result keeps assumption failure of an earlier parameter`() = runTest {
        val parameterizedTest = stubTest(method = "parameterized")
        val listener = createListener(stubTestBatch(parameterizedTest))

        listener.testRunStarted("run", 2)
        listener.testStarted(stubTest(method = "parameterized[0]"))
        listener.testAssumptionFailure(stubTest(method = "parameterized[0]"), "assumption trace")
        listener.testEnded(stubTest(method = "parameterized[0]"), emptyMap())
        listener.testStarted(stubTest(method = "parameterized[1]"))
        listener.testEnded(stubTest(method = "parameterized[1]"), emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test).isEqualTo(parameterizedTest)
        assertThat(finishedResult.status).isEqualTo(TestStatus.ASSUMPTION_FAILURE)
        assertThat(results.failed).isEmpty()
    }

    @Test
    fun `merged parameterized result stays incomplete when a parameter never ended`() = runTest {
        val parameterizedTest = stubTest(method = "parameterized")
        val listener = createListener(stubTestBatch(parameterizedTest))

        listener.testRunStarted("run", 2)
        listener.testStarted(stubTest(method = "parameterized[0]"))
        listener.testStarted(stubTest(method = "parameterized[1]"))
        listener.testEnded(stubTest(method = "parameterized[1]"), emptyMap())
        listener.testRunFailed("process crashed")

        val results = deferred.await()
        val uncompletedResult = results.uncompleted.single()
        assertThat(uncompletedResult.test).isEqualTo(parameterizedTest)
        assertThat(uncompletedResult.status).isEqualTo(TestStatus.INCOMPLETE)
        assertThat(results.finished).isEmpty()
        assertThat(results.failed).isEmpty()
    }

    @Test
    fun `does not merge results when the batch explicitly contains parameterized tests`() = runTest {
        val param0 = stubTest(method = "parameterized[0]")
        val param1 = stubTest(method = "parameterized[1]")
        val listener = createListener(stubTestBatch(param0, param1))

        listener.testRunStarted("run", 2)
        listener.testStarted(param0)
        listener.testEnded(param0, emptyMap())
        listener.testStarted(param1)
        listener.testEnded(param1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        assertThat(results.finished).flatExtracting({ it.test }).containsExactlyInAnyOrder(param0, param1)
        assertThat(results.uncompleted).isEmpty()
        verify(progressReporter, never()).addTestDiscoveredDuringRuntime(any(), any())
    }

    @Test
    fun `keeps the results reported at the first run event after a run failure`() = runTest {
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testRunFailed("run crashed")
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val uncompletedResult = results.uncompleted.single()
        assertThat(uncompletedResult.test).isEqualTo(test1)
        assertThat(uncompletedResult.status).isEqualTo(TestStatus.INCOMPLETE)
        assertThat(results.finished).isEmpty()
    }

    @Test
    fun `completes the batch results when the run is stopped`() = runTest {
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testRunStopped(100)

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test).isEqualTo(test1)
        assertThat(finishedResult.status).isEqualTo(TestStatus.PASSED)
        assertThat(results.uncompleted).isEmpty()
    }

    @Test
    fun `marks result as strict when the strict run checker reports it as strict`() = runTest {
        whenever(strictRunChecker.isStrictRun(test1)).thenReturn(true)
        val listener = createListener(stubTestBatch(test1))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.isStrictRun).isTrue()
    }

    @Test
    fun `uses the test instance from the batch for the result`() = runTest {
        val batchTest = test1.copy(metaProperties = listOf(MetaProperty("org.junit.Ignore")))
        val listener = createListener(stubTestBatch(batchTest))

        listener.testRunStarted("run", 1)
        listener.testStarted(test1)
        listener.testEnded(test1, emptyMap())
        listener.testRunEnded(100, emptyMap())

        val results = deferred.await()
        val finishedResult = results.finished.single()
        assertThat(finishedResult.test.metaProperties).containsExactly(MetaProperty("org.junit.Ignore"))
    }

    private fun createListener(
        batch: TestBatch,
        attachmentProviders: List<AttachmentProvider> = emptyList()
    ): TestRunResultsListener = TestRunResultsListener(
        testBatch = batch,
        device = device,
        deferred = deferred,
        timer = timer,
        progressReporter = progressReporter,
        poolId = poolId,
        strictRunChecker = strictRunChecker,
        attachmentProviders = attachmentProviders
    )

    private class TestAttachmentProvider : AttachmentProvider {

        private lateinit var listener: AttachmentListener

        override fun registerListener(listener: AttachmentListener) {
            this.listener = listener
        }

        fun send(test: MarathonTest, attachment: Attachment) {
            listener.onAttachment(test, attachment)
        }
    }
}
