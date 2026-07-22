package com.malinskiy.marathon.report.trace

import com.malinskiy.marathon.analytics.internal.sub.CacheLoadEvent
import com.malinskiy.marathon.analytics.internal.sub.CacheStoreEvent
import com.malinskiy.marathon.analytics.internal.sub.DeviceProviderPreparingEvent
import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.analytics.internal.sub.stubExecutionReport
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.stubDeviceInfo
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.report.trace.chrome.CompleteEvent
import com.malinskiy.marathon.report.trace.chrome.TraceReport
import com.malinskiy.marathon.report.trace.chrome.TraceReportClient
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class TraceReporterTest {
    @TempDir
    private lateinit var outputDir: File

    private val reporter by lazy { TraceReporter(outputDir) }

    @Test
    fun `overlapping cache events are placed on separate lanes`() = runTest {
        val report = stubExecutionReport(
            cacheStoreEvent = listOf(
                CacheStoreEvent(start = Instant.ofEpochMilli(60), finish = Instant.ofEpochMilli(200), test = stubTest(method = "test3"))
            ),
            cacheLoadEvent = listOf(
                CacheLoadEvent(start = Instant.ofEpochMilli(0), finish = Instant.ofEpochMilli(100), test = stubTest(method = "test1")),
                CacheLoadEvent(start = Instant.ofEpochMilli(50), finish = Instant.ofEpochMilli(150), test = stubTest(method = "test2"))
            )
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting(
                { it.eventName },
                { it.args?.get("test_name") },
                { it.threadId },
                { it.timestampMicroseconds },
                { (it as CompleteEvent).durationMicroseconds }
            )
            .containsExactlyInAnyOrder(
                tuple("cache_load", "SimpleTest.test1", "caches-01", 0L, 100_000L),
                tuple("cache_load", "SimpleTest.test2", "caches-02", 50_000L, 100_000L),
                tuple("cache_store", "SimpleTest.test3", "caches-03", 60_000L, 140_000L)
            )
    }

    @Test
    fun `a cache lane is reused after its previous event finishes`() = runTest {
        val report = stubExecutionReport(
            cacheLoadEvent = listOf(
                CacheLoadEvent(start = Instant.ofEpochMilli(0), finish = Instant.ofEpochMilli(100), test = stubTest(method = "test1")),
                CacheLoadEvent(start = Instant.ofEpochMilli(50), finish = Instant.ofEpochMilli(150), test = stubTest(method = "test2")),
                CacheLoadEvent(start = Instant.ofEpochMilli(120), finish = Instant.ofEpochMilli(180), test = stubTest(method = "test3"))
            )
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting({ it.args?.get("test_name") }, { it.threadId })
            .containsExactlyInAnyOrder(
                tuple("SimpleTest.test1", "caches-01"),
                tuple("SimpleTest.test2", "caches-02"),
                tuple("SimpleTest.test3", "caches-01")
            )
    }

    @Test
    fun `sequential cache events share a single lane`() = runTest {
        val report = stubExecutionReport(
            cacheStoreEvent = listOf(
                CacheStoreEvent(start = Instant.ofEpochMilli(100), finish = Instant.ofEpochMilli(200), test = stubTest(method = "test1"))
            ),
            cacheLoadEvent = listOf(
                CacheLoadEvent(start = Instant.ofEpochMilli(0), finish = Instant.ofEpochMilli(100), test = stubTest(method = "test1"))
            )
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting<String?> { it.threadId }
            .containsOnly("caches-01")
    }

    @Test
    fun `a zero-duration cache event shares a lane with an event starting at the same instant`() = runTest {
        val report = stubExecutionReport(
            cacheLoadEvent = listOf(
                CacheLoadEvent(start = Instant.ofEpochMilli(50), finish = Instant.ofEpochMilli(50), test = stubTest(method = "test1")),
                CacheLoadEvent(start = Instant.ofEpochMilli(50), finish = Instant.ofEpochMilli(150), test = stubTest(method = "test2"))
            )
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting<String?> { it.threadId }
            .containsOnly("caches-01")
    }

    @Test
    fun `lane numbers are padded to the widest lane index`() = runTest {
        val report = stubExecutionReport(
            cacheLoadEvent = (1..100).map {
                CacheLoadEvent(start = Instant.ofEpochMilli(0), finish = Instant.ofEpochMilli(100), test = stubTest(method = "test$it"))
            }
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting<String?> { it.threadId }
            .doesNotHaveDuplicates()
            .contains("caches-001", "caches-100")
    }

    @Test
    fun `device provider preparing is rendered on a dedicated provider lane`() = runTest {
        val report = stubExecutionReport(
            deviceProviderPreparingEvent = listOf(
                DeviceProviderPreparingEvent(start = Instant.ofEpochMilli(0), finish = Instant.ofEpochMilli(100), serialNumber = "emulator-5554")
            )
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting({ it.eventName }, { it.threadId })
            .containsExactly(tuple("device_provider_preparing", "emulator-5554-provider"))
    }

    @Test
    fun `cached test result is rendered on the cache-hits lane`() = runTest {
        val report = stubExecutionReport(
            testEvents = listOf(
                TestEvent(
                    instant = Instant.ofEpochMilli(500),
                    poolId = DevicePoolId("myPool"),
                    device = stubDeviceInfo(serialNumber = "emulator-5554"),
                    testResult = stubTestResult(isFromCache = true),
                    final = true
                )
            )
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting({ it.eventName }, { it.threadId }, { it.timestampMicroseconds }, { (it as CompleteEvent).durationMicroseconds })
            .containsExactly(tuple("test", "cache-hits", 0L, 0L))
    }

    @Test
    fun `executed test result is rendered on the device lane`() = runTest {
        val report = stubExecutionReport(
            testEvents = listOf(
                TestEvent(
                    instant = Instant.ofEpochMilli(500),
                    poolId = DevicePoolId("myPool"),
                    device = stubDeviceInfo(serialNumber = "emulator-5554"),
                    testResult = stubTestResult(startTime = 600, endTime = 700),
                    final = true
                )
            )
        )

        reporter.generate(report)

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents)
            .extracting({ it.eventName }, { it.threadId })
            .containsExactly(tuple("test", "emulator-5554"))
    }

    @Test
    fun `empty report produces an empty trace`() = runTest {
        reporter.generate(stubExecutionReport())

        val traceEvents = readTraceReport().traceEvents
        assertThat(traceEvents).isEmpty()
    }

    private fun readTraceReport(): TraceReport = TraceReportClient().readFrom(File(outputDir, "trace/timeline.trace"))
}
