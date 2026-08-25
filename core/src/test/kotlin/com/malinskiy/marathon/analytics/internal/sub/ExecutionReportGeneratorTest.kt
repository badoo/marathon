package com.malinskiy.marathon.analytics.internal.sub

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.stubDeviceInfo
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.report.Reporter
import com.malinskiy.marathon.report.StubReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class ExecutionReportGeneratorTest {
    @Test
    fun `every reporter in the list is run`() = runTest {
        val ran = mutableListOf<String>()
        val reporters = (1..5).map { RecordingReporter(name = "r-$it", log = ran) }
        val generator = createExecutionReportGenerator(reporters)

        generator.finish()

        assertThat(ran).containsExactlyInAnyOrder("r-1", "r-2", "r-3", "r-4", "r-5")
    }

    @Test
    fun `reporters run concurrently`() = runTest {
        val ran = mutableListOf<String>()
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val generator = createExecutionReportGenerator(
            reporters = listOf(
                RecordingReporter(name = "first", log = ran) {
                    firstStarted.complete(Unit)
                    secondStarted.await()
                },
                RecordingReporter(name = "second", log = ran) {
                    secondStarted.complete(Unit)
                    firstStarted.await()
                },
            ),
        )

        generator.finish()

        assertThat(ran).containsExactlyInAnyOrder("first", "second")
    }

    @Test
    fun `a failing reporter does not stop the others and finish rethrows it`() = runTest {
        val ran = mutableListOf<String>()
        val boom = IllegalStateException("boom")
        val generator = createExecutionReportGenerator(
            reporters = listOf(
                RecordingReporter(name = "before", log = ran),
                FailingReporter(error = boom),
                RecordingReporter(name = "after", log = ran),
                RecordingReporter(name = "last", log = ran),
            ),
        )

        val thrown = assertThrows<IllegalStateException> { generator.finish() }
        assertThat(thrown).isSameAs(boom)
        assertThat(ran).containsExactlyInAnyOrder("before", "after", "last")
    }

    @Test
    fun `failures are aggregated with the first reporter in list order as the primary`() = runTest {
        val first = IllegalStateException("first")
        val second = IllegalStateException("second")
        val secondFailed = CompletableDeferred<Unit>()
        val generator = createExecutionReportGenerator(
            reporters = listOf(
                RecordingReporter(name = "first", log = mutableListOf()) {
                    secondFailed.await()
                    throw first
                },
                RecordingReporter(name = "second", log = mutableListOf()) {
                    try {
                        throw second
                    } finally {
                        secondFailed.complete(Unit)
                    }
                },
            ),
        )

        val thrown = assertThrows<IllegalStateException> { generator.finish() }

        assertThat(thrown).isSameAs(first)
        assertThat(thrown.suppressed).containsExactly(second)
    }

    @Test
    fun `a reporter cancelling only its own coroutine is not collected as a failure`() = runTest {
        val ran = mutableListOf<String>()
        val generator = createExecutionReportGenerator(
            reporters = listOf(
                RecordingReporter(name = "before", log = ran),
                SelfCancellingReporter(),
                RecordingReporter(name = "after", log = ran),
            ),
        )

        generator.finish()

        assertThat(ran).containsExactlyInAnyOrder("before", "after")
    }

    @Test
    fun `cancelling finish cancels cooperatively without hanging`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val neverCompletes = CompletableDeferred<Unit>()
        val generator = createExecutionReportGenerator(
            reporters = listOf(
                RecordingReporter(name = "blocking", log = mutableListOf()) {
                    entered.complete(Unit)
                    neverCompletes.await()
                },
            ),
        )
        val job = backgroundScope.launch { generator.finish() }

        entered.await()
        job.cancelAndJoin()

        assertThat(job.isCancelled).isTrue()
    }

    @Test
    fun `test events are inflated and sorted by time before being reported`() = runTest {
        val reporter = StubReporter()
        val markAsFinal = object : TestEventInflator {
            override suspend fun inflate(event: TestEvent): TestEvent = event.copy(final = true)
        }
        val generator = createExecutionReportGenerator(
            reporters = listOf(reporter),
            testEventInflators = listOf(markAsFinal),
        )
        generator.track(testEvent(pool = "third", startTime = 300, endTime = 400))
        generator.track(testEvent(pool = "first", startTime = 100, endTime = 200))
        generator.track(testEvent(pool = "second", startTime = 0, endTime = 0, instant = Instant.ofEpochMilli(200)))
        generator.finish()
        val report = reporter.reports.single()

        assertThat(report.testEvents).flatExtracting({ it.poolId.name }).containsExactly("first", "second", "third")
        assertThat(report.testEvents).allMatch { it.final }
    }

    private fun testEvent(
        pool: String,
        startTime: Long,
        endTime: Long,
        instant: Instant = Instant.ofEpochMilli(0),
    ) = TestEvent(
        instant = instant,
        poolId = DevicePoolId(pool),
        device = stubDeviceInfo(),
        testResult = stubTestResult(startTime = startTime, endTime = endTime),
        final = false,
    )

    private fun TestScope.createExecutionReportGenerator(reporters: List<Reporter> = emptyList(), testEventInflators: List<TestEventInflator> = emptyList()) =
        ExecutionReportGenerator(
            reporters = reporters,
            testEventInflators = testEventInflators,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

    private class RecordingReporter(
        private val name: String,
        private val log: MutableList<String>,
        private val onGenerate: suspend () -> Unit = {},
    ) : Reporter {
        override suspend fun generate(executionReport: ExecutionReport) {
            onGenerate()
            synchronized(log) { log.add(name) }
        }
    }

    private class FailingReporter(private val error: Exception) : Reporter {
        override suspend fun generate(executionReport: ExecutionReport): Unit = throw error
    }

    private class SelfCancellingReporter : Reporter {
        override suspend fun generate(executionReport: ExecutionReport) {
            currentCoroutineContext().cancel()
            currentCoroutineContext().ensureActive()
        }
    }
}
