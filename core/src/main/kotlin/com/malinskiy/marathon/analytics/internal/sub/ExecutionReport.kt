package com.malinskiy.marathon.analytics.internal.sub

import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.report.summary.Batch
import com.malinskiy.marathon.report.summary.TestSummary
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toTestName
import java.time.Instant

/**
 * Events are sorted by timestamp
 */
data class ExecutionReport(
    val deviceConnectedEvents: List<DeviceConnectedEvent>,
    val devicePreparingEvents: List<DevicePreparingEvent>,
    val deviceProviderPreparingEvent: List<DeviceProviderPreparingEvent>,
    val installCheckEvent: List<InstallationCheckEvent>,
    val installEvent: List<InstallationEvent>,
    val executeBatchEvent: List<ExecutingBatchEvent>,
    val cacheStoreEvent: List<CacheStoreEvent>,
    val cacheLoadEvent: List<CacheLoadEvent>,
    val testEvents: List<TestEvent>
) {
    val summary: Summary by lazy {
        val pools = deviceConnectedEvents.map { it.poolId }.distinct()
        val poolsSummary: List<PoolSummary> = pools.map { compilePoolSummary(it) }
        Summary(poolsSummary)
    }

    val testSummaries: Map<Test, TestSummary> by lazy {
        createTestSummaries()
    }

    val allEvents: List<Event> by lazy {
        listOf(
            deviceConnectedEvents,
            devicePreparingEvents,
            devicePreparingEvents,
            installCheckEvent,
            installEvent,
            executeBatchEvent,
            cacheStoreEvent,
            cacheLoadEvent,
            testEvents
        )
            .flatten()
    }

    private fun createTestSummaries(): Map<Test, TestSummary> {
        val resultsByTest: Map<Test, List<TestResult>> = testEvents
            .groupBy(keySelector = { it.testResult.test }, valueTransform = { it.testResult })

        val batches: Map<String, Batch> = testEvents
            .groupBy(keySelector = { it.testResult.batchId }, valueTransform = { it.testResult })
            .map { it.key to Batch(batchId = it.key, testResults = it.value) }
            .toMap()

        val summaries = hashMapOf<Test, TestSummary>()

        testEvents.forEach {
            val test = it.testResult.test
            if (test !in summaries) {
                val allTestRuns = resultsByTest[test] ?: emptyList()
                val allBatches = allTestRuns
                    .map { result -> result.batchId }
                    .toSet()
                    .map { batchId -> batches.getValue(batchId) }

                summaries[test] = TestSummary(test, allTestRuns, allBatches)
            }
        }

        return summaries
    }

    @Suppress("LongMethod")
    private fun compilePoolSummary(poolId: DevicePoolId): PoolSummary {
        val devices = deviceConnectedEvents.filter { it.poolId == poolId }.map { it.device }.distinctBy { it.serialNumber }

        val poolTestEvents = testEvents.filter { it.poolId == poolId }
        val poolTestFinalEvents = poolTestEvents.filter { it.final }

        val tests = poolTestFinalEvents
            .map { it.testResult }
            .filter { it.status != TestStatus.INCOMPLETE }

        val passed = tests
            .filter { it.status == TestStatus.PASSED }
            .map { it.test.toTestName() }
            .toSet()

        val ignored = tests
            .filter { it.status == TestStatus.IGNORED || it.status == TestStatus.ASSUMPTION_FAILURE }
            .map { it.test.toTestName() }
            .toSet()

        val failed = tests
            .filter {
                it.status != TestStatus.PASSED
                    && it.status != TestStatus.IGNORED
                    && it.status != TestStatus.ASSUMPTION_FAILURE
            }.map { it.test.toTestName() }
            .toSet()

        val fromCache = tests
            .filter { it.isFromCache }
            .map { it.test.toTestName() }
            .toSet()

        val duration = tests.map { it.durationMillis() }.sum()

        val rawTests = poolTestEvents
            .map { it.testResult }
        val rawPassed = rawTests
            .filter { it.status == TestStatus.PASSED }
            .map { it.test.toTestName() }

        val rawIgnored = rawTests
            .filter { it.status == TestStatus.IGNORED || it.status == TestStatus.ASSUMPTION_FAILURE }
            .map { it.test.toTestName() }

        val rawFailed = rawTests
            .filter { it.status == TestStatus.FAILURE }
            .map { it.test.toTestName() }

        val rawIncomplete = rawTests
            .filter { it.status == TestStatus.INCOMPLETE }
            .map { it.test.toTestName() }

        val rawDuration = rawTests
            //Incomplete tests mess up the calculations of time since their end time is 0 and duration is, hence, years
            //We filter here for unavailable time just to be safe
            .filter { it.startTime != 0L && it.endTime != 0L }
            .map { it.durationMillis() }.sum()

        val retries = tests.map { result: TestResult ->
            Pair(result, poolTestEvents.filter { it.testResult.test == result.test && it.testResult !== result })
        }.toMap()

        return PoolSummary(
            poolId = poolId,
            tests = tests,
            retries = retries,
            passed = passed,
            ignored = ignored,
            fromCache = fromCache,
            failed = failed,
            flaky = 0,
            durationMillis = duration,
            devices = devices,
            rawPassed = rawPassed,
            rawFailed = rawFailed,
            rawIgnored = rawIgnored,
            rawIncomplete = rawIncomplete,
            rawDurationMillis = rawDuration
        )
    }
}

sealed class Event

data class DeviceConnectedEvent(
    val instant: Instant,
    val poolId: DevicePoolId,
    val device: DeviceInfo
) : Event()

data class DevicePreparingEvent(
    val start: Instant,
    val finish: Instant,
    val serialNumber: String
) : Event()

data class DeviceProviderPreparingEvent(
    val start: Instant,
    val finish: Instant,
    val serialNumber: String
) : Event()

data class InstallationCheckEvent(
    val start: Instant,
    val finish: Instant,
    val serialNumber: String
) : Event()

data class InstallationEvent(
    val start: Instant,
    val finish: Instant,
    val serialNumber: String
) : Event()

data class ExecutingBatchEvent(
    val start: Instant,
    val finish: Instant,
    val serialNumber: String
) : Event()

data class CacheStoreEvent(
    val start: Instant,
    val finish: Instant,
    val test: Test
) : Event()

data class CacheLoadEvent(
    val start: Instant,
    val finish: Instant,
    val test: Test
) : Event()

data class TestEvent(
    val instant: Instant,
    val poolId: DevicePoolId,
    val device: DeviceInfo,
    val testResult: TestResult,
    val final: Boolean
) : Event()
