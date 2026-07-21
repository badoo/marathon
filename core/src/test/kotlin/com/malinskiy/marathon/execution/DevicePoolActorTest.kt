package com.malinskiy.marathon.execution

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.DevicePoolMessage.FromScheduler
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.report.logs.StubLogsProvider
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.malinskiy.marathon.test.Test as MarathonTest

class DevicePoolActorTest {
    private val poolId = DevicePoolId("test-pool")
    private val track = mock<Track>()
    private val timer = mock<Timer>()
    private val parentJob = Job()

    @AfterEach
    fun tearDown() {
        parentJob.cancel()
    }

    @Test
    fun `added device executes queued tests and reports results`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100)
        val test = stubTest(method = "test1")
        device.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        val pool = createPoolActor()

        pool.send(FromScheduler.AddDevice(device))
        pool.send(FromScheduler.AddTests(TestShard(listOf(test))))
        pool.send(FromScheduler.RequestStop)
        awaitTermination(pool)

        val testResultCaptor = argumentCaptor<TestResult>()
        verify(track).test(eq(poolId), eq(device.toDeviceInfo()), testResultCaptor.capture(), eq(true))
        assertThat(testResultCaptor.firstValue.test).isEqualTo(test)
        assertThat(testResultCaptor.firstValue.status).isEqualTo(TestStatus.PASSED)
        assertThat(device.executionIndexMap).containsOnlyKeys(test)
    }

    @Test
    fun `tests are distributed across pool devices`() = runTest {
        val device1 = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "serial-1")
        val device2 = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "serial-2")
        val tests = (1..4).map { stubTest(method = "test$it") }
        val executionResults = tests.associateWith { arrayOf(TestStatus.PASSED) }
        device1.executionResults = executionResults
        device2.executionResults = executionResults
        val pool = createPoolActor()

        pool.send(FromScheduler.AddDevice(device1))
        pool.send(FromScheduler.AddDevice(device2))
        pool.send(FromScheduler.AddTests(TestShard(tests)))
        pool.send(FromScheduler.RequestStop)
        awaitTermination(pool)

        val testResultCaptor = argumentCaptor<TestResult>()
        verify(track, times(4)).test(eq(poolId), any(), testResultCaptor.capture(), eq(true))
        assertThat(testResultCaptor.allValues).extracting<MarathonTest> { it.test }.containsExactlyInAnyOrderElementsOf(tests)
        assertThat(device1.executionIndexMap).isNotEmpty()
        assertThat(device2.executionIndexMap).isNotEmpty()
    }

    @Test
    fun `device with a duplicate serial number is not added to the pool`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "serial-1")
        val duplicate = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "serial-1")
        val test = stubTest(method = "test1")
        device.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        duplicate.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        val pool = createPoolActor()

        pool.send(FromScheduler.AddDevice(device))
        pool.send(FromScheduler.AddDevice(duplicate))
        pool.send(FromScheduler.AddTests(TestShard(listOf(test))))
        pool.send(FromScheduler.RequestStop)
        awaitTermination(pool)

        assertThat(device.executionIndexMap).containsOnlyKeys(test)
        assertThat(duplicate.executionIndexMap).isEmpty()
    }

    @Test
    fun `removed device does not receive new work`() = runTest {
        val removed = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "removed")
        val remaining = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "remaining")
        val test = stubTest(method = "test1")
        removed.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        remaining.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        val pool = createPoolActor()

        pool.send(FromScheduler.AddDevice(removed))
        pool.send(FromScheduler.RemoveDevice(removed))
        pool.send(FromScheduler.AddDevice(remaining))
        pool.send(FromScheduler.AddTests(TestShard(listOf(test))))
        pool.send(FromScheduler.RequestStop)
        awaitTermination(pool)

        assertThat(removed.executionIndexMap).isEmpty()
        assertThat(remaining.executionIndexMap).containsOnlyKeys(test)
    }

    @Test
    fun `batch returned by a crashing device is retried until the quota is exhausted`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, crashWithTestBatchException = true)
        val test = stubTest(method = "test1")
        device.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        val pool = createPoolActor(configuration { uncompletedTestRetryQuota = 1 })

        pool.send(FromScheduler.AddDevice(device))
        pool.send(FromScheduler.AddTests(TestShard(listOf(test))))
        pool.send(FromScheduler.RequestStop)
        awaitTermination(pool)

        val testResultCaptor = argumentCaptor<TestResult>()
        verify(track).test(eq(poolId), any(), testResultCaptor.capture(), eq(true))
        assertThat(testResultCaptor.firstValue.test).isEqualTo(test)
        assertThat(testResultCaptor.firstValue.status).isEqualTo(TestStatus.FAILURE)
        assertThat(device.executionIndexMap).isEmpty()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `pool terminates after the no-devices timeout when stop was requested`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 10_000)
        val test1 = stubTest(method = "test1")
        val test2 = stubTest(method = "test2")
        device.executionResults = mapOf(test1 to arrayOf(TestStatus.PASSED), test2 to arrayOf(TestStatus.PASSED))
        val pool = createPoolActor()

        pool.send(FromScheduler.AddDevice(device))
        pool.send(FromScheduler.AddTests(TestShard(listOf(test1, test2))))
        pool.send(FromScheduler.RequestStop)
        advanceTimeBy(5.seconds)
        pool.send(FromScheduler.RemoveDevice(device))
        advanceTimeBy(4.minutes)

        assertThat(pool.isClosedForSend).isFalse()

        advanceTimeBy(2.minutes)
        runCurrent()

        assertThat(pool.isClosedForSend).isTrue()
    }

    private fun TestScope.createPoolActor(configuration: Configuration = configuration()): DevicePoolActor =
        DevicePoolActor(
            poolId = poolId,
            configuration = configuration,
            analytics = Analytics(NoOpMetricsProvider()),
            progressReporter = ProgressReporter(configuration),
            track = track,
            timer = timer,
            logsProvider = StubLogsProvider(),
            strictRunChecker = ConfigurationStrictRunChecker(configuration),
            parent = parentJob,
            context = StandardTestDispatcher(testScheduler)
        )

    private suspend fun awaitTermination(pool: DevicePoolActor) {
        while (!pool.isClosedForSend) {
            delay(100.milliseconds)
        }
    }
}
