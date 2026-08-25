package com.malinskiy.marathon.execution

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.cache.CacheService
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.cache.test.CacheResult
import com.malinskiy.marathon.cache.test.CacheTestReporter
import com.malinskiy.marathon.cache.test.TestCacheLoader
import com.malinskiy.marathon.cache.test.TestCacheSaver
import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.device.StubDeviceProvider
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.exceptions.NoDevicesException
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.report.logs.StubLogsProvider
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URI
import com.malinskiy.marathon.test.Test as MarathonTest

class SchedulerTest {
    private val omniPoolId = DevicePoolId("omni")
    private val deviceProvider = StubDeviceProvider()
    private val cacheService = mock<CacheService>()
    private val cacheLoader = mock<TestCacheLoader>()
    private val cacheSaver = mock<TestCacheSaver>()
    private val cachedTestsReporter = mock<CacheTestReporter>()
    private val track = mock<Track>()
    private val timer = mock<Timer>()

    @Test
    fun `initialize creates a pool for the connected device`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100)
        deviceProvider.coroutineScope = this
        deviceProvider.providingLogic = { it.send(DeviceEvent.DeviceConnected(device)) }
        val scheduler = createScheduler(configuration())

        scheduler.initialize()
        scheduler.stopAndWaitForCompletion()

        verify(track).deviceConnected(omniPoolId, device.toDeviceInfo())
    }

    @Test
    fun `initialize throws when no device connects within the timeout`() = runTest {
        deviceProvider.coroutineScope = this
        val scheduler = createScheduler(configuration())

        assertThrows<NoDevicesException> { scheduler.initialize() }

        deviceProvider.terminate()
    }

    @Test
    fun `devices not matching the serial number filters are ignored`() = runTest {
        val allowed = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "allowed-1")
        val excluded = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "allowed-excluded")
        val notIncluded = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "other-1")
        deviceProvider.coroutineScope = this
        deviceProvider.providingLogic = {
            it.send(DeviceEvent.DeviceConnected(excluded))
            it.send(DeviceEvent.DeviceConnected(notIncluded))
            it.send(DeviceEvent.DeviceConnected(allowed))
        }
        val configuration = configuration {
            includeSerialRegexes = listOf("allowed.*".toRegex())
            excludeSerialRegexes = listOf(".*excluded".toRegex())
        }
        val scheduler = createScheduler(configuration)

        scheduler.initialize()
        scheduler.stopAndWaitForCompletion()

        verify(track).deviceConnected(any(), any())
        verify(track).deviceConnected(omniPoolId, allowed.toDeviceInfo())
    }

    @Test
    fun `tests are not assigned to a device that disconnected`() = runTest {
        val disconnected = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "disconnected")
        val remaining = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100, serialNumber = "remaining")
        val tests = (1..4).map { stubTest(method = "test$it") }
        val executionResults = tests.associateWith { arrayOf(TestStatus.PASSED) }
        disconnected.executionResults = executionResults
        remaining.executionResults = executionResults
        deviceProvider.coroutineScope = this
        deviceProvider.providingLogic = {
            it.send(DeviceEvent.DeviceConnected(disconnected))
            it.send(DeviceEvent.DeviceConnected(remaining))
            it.send(DeviceEvent.DeviceDisconnected(disconnected))
        }
        val scheduler = createScheduler(configuration())

        scheduler.initialize()
        scheduler.addTests(TestShard(tests))
        scheduler.stopAndWaitForCompletion()

        assertThat(disconnected.executionIndexMap).isEmpty()
        assertThat(remaining.executionIndexMap).containsOnlyKeys(*tests.toTypedArray())
    }

    @Test
    fun `a device that reconnects after a disconnect is prepared again and receives tests`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100)
        val test = stubTest(method = "test1")
        device.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        deviceProvider.coroutineScope = this
        deviceProvider.providingLogic = {
            it.send(DeviceEvent.DeviceConnected(device))
            delay(1_000)
            it.send(DeviceEvent.DeviceDisconnected(device))
            it.send(DeviceEvent.DeviceConnected(device))
        }
        val scheduler = createScheduler(configuration())

        scheduler.initialize()
        delay(2_000)
        scheduler.addTests(TestShard(listOf(test)))
        scheduler.stopAndWaitForCompletion()

        assertThat(device.prepareCount).isEqualTo(2)
        assertThat(device.executionIndexMap).containsOnlyKeys(test)
    }

    @Test
    fun `addTests sends the shard to the pools when cache is disabled`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100)
        val test = stubTest(method = "test1")
        device.executionResults = mapOf(test to arrayOf(TestStatus.PASSED))
        deviceProvider.coroutineScope = this
        deviceProvider.providingLogic = { it.send(DeviceEvent.DeviceConnected(device)) }
        val scheduler = createScheduler(configuration())

        scheduler.initialize()
        scheduler.addTests(TestShard(listOf(test)))
        scheduler.stopAndWaitForCompletion()

        assertThat(device.executionIndexMap).containsOnlyKeys(test)
        verify(cacheLoader, never()).addTests(any(), any())
    }

    @Test
    fun `addTests routes tests through the cache loader when cache is enabled`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100)
        deviceProvider.coroutineScope = this
        deviceProvider.providingLogic = { it.send(DeviceEvent.DeviceConnected(device)) }
        val scheduler = createScheduler(cacheEnabledConfiguration())
        val shard = TestShard(listOf(stubTest(method = "test1")))

        scheduler.initialize()
        scheduler.addTests(shard)
        scheduler.stopAndWaitForCompletion()

        verify(cacheLoader).start(any(), any())
        verify(cacheLoader).addTests(omniPoolId, shard)
        verify(cacheLoader).stop()
        verify(cacheSaver).terminate()
    }

    @Test
    fun `cache results are reported for hits and scheduled for misses`() = runTest {
        val device = StubDevice(prepareTimeMillis = 100, testTimeMillis = 100)
        val missTest = stubTest(method = "miss")
        val hitTest = stubTest(method = "hit")
        device.executionResults = mapOf(missTest to arrayOf(TestStatus.PASSED))
        deviceProvider.coroutineScope = this
        deviceProvider.providingLogic = { it.send(DeviceEvent.DeviceConnected(device)) }
        var consumer: (suspend (CacheResult) -> Unit)? = null
        whenever(cacheLoader.start(any(), any())).thenAnswer { invocation ->
            consumer = invocation.getArgument(1)
        }
        val scheduler = createScheduler(cacheEnabledConfiguration())
        scheduler.initialize()
        val hitResult = testResult(hitTest, device)

        consumer?.invoke(CacheResult.Hit(omniPoolId, hitResult))
        consumer?.invoke(CacheResult.Miss(omniPoolId, missTest))
        scheduler.stopAndWaitForCompletion()

        verify(cachedTestsReporter).onCachedTest(omniPoolId, hitResult)
        assertThat(device.executionIndexMap).containsOnlyKeys(missTest)
    }

    @Test
    fun `close closes the cache dependencies`() {
        val scheduler = createScheduler(configuration())

        scheduler.close()

        verify(cacheService).close()
        verify(cacheLoader).close()
        verify(cacheSaver).close()
    }

    private fun createScheduler(configuration: Configuration): Scheduler = Scheduler(
        deviceProvider = deviceProvider,
        cacheService = cacheService,
        cacheLoader = cacheLoader,
        cacheSaver = cacheSaver,
        cachedTestsReporter = cachedTestsReporter,
        analytics = Analytics(NoOpMetricsProvider()),
        configuration = configuration,
        progressReporter = ProgressReporter(configuration.strictMode),
        strictRunChecker = ConfigurationStrictRunChecker(configuration),
        logsProvider = StubLogsProvider(),
        track = track,
        timer = timer,
    )

    private fun cacheEnabledConfiguration(): Configuration = configuration {
        cache = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = URI("http://localhost/cache"), push = true))
    }

    private fun testResult(test: MarathonTest, device: StubDevice): TestResult =
        stubTestResult(test = test, device = device.toDeviceInfo(), endTime = 1, batchId = "cached-batch")
}
