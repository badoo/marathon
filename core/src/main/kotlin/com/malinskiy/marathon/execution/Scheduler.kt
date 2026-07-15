package com.malinskiy.marathon.execution

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.cache.CacheService
import com.malinskiy.marathon.cache.test.CacheResult
import com.malinskiy.marathon.cache.test.CacheTestReporter
import com.malinskiy.marathon.cache.test.TestCacheLoader
import com.malinskiy.marathon.cache.test.TestCacheSaver
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.exceptions.NoDevicesException
import com.malinskiy.marathon.execution.DevicePoolMessage.FromScheduler
import com.malinskiy.marathon.execution.DevicePoolMessage.FromScheduler.AddDevice
import com.malinskiy.marathon.execution.DevicePoolMessage.FromScheduler.RemoveDevice
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * The logic of scheduler:
 * 1) Subscribe on DeviceProvider
 * 2) Create device pools using PoolingStrategy
 */
class Scheduler(
    private val deviceProvider: DeviceProvider,
    private val cacheService: CacheService,
    private val cacheLoader: TestCacheLoader,
    private val cacheSaver: TestCacheSaver,
    private val cachedTestsReporter: CacheTestReporter,
    private val analytics: Analytics,
    private val configuration: Configuration,
    private val progressReporter: ProgressReporter,
    private val strictRunChecker: StrictRunChecker,
    private val logsProvider: LogsProvider,
    private val track: Track,
    private val timer: Timer
) : AutoCloseable {

    private val job = Job()
    private val pools = ConcurrentHashMap<DevicePoolId, SendChannel<FromScheduler>>()
    private val poolingStrategy = configuration.poolingStrategy

    private val logger = MarathonLogging.getLogger(Scheduler::class.java)

    suspend fun initialize() {
        val scope = CoroutineScope(coroutineContext)
        initializeDeviceProvider(scope)
        initializeCache(scope)

        try {
            withTimeout(configuration.noDevicesTimeoutMillis) {
                while (pools.isEmpty()) {
                    logger.debug("Waiting for a device...")
                    delay(500L)
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw NoDevicesException(e)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun stopAndWaitForCompletion() {
        if (configuration.cache.isEnabled) {
            cacheLoader.stop()
        }

        logger.debug("Requesting stop in pools")

        pools.values.forEach {
            if (!it.isClosedForSend) {
                it.send(FromScheduler.RequestStop)
            }
        }

        for (child in job.children) {
            child.join()
        }

        if (configuration.cache.isPushEnabled) {
            cacheSaver.terminate()
        }

        deviceProvider.terminate()
    }

    suspend fun addTests(shard: TestShard) {
        if (configuration.cache.isEnabled) {
            pools.keys.forEach { pool ->
                cacheLoader.addTests(pool, shard)
            }
        } else {
            pools.values.forEach {
                it.send(FromScheduler.AddTests(shard))
            }
        }
    }

    override fun close() {
        deviceProvider.close()
        cacheLoader.close()
        cacheSaver.close()
        cacheService.close()
    }

    private fun initializeCache(scope: CoroutineScope) {
        logger.debug("Test cache is ${if (configuration.cache.isEnabled) "enabled" else "disabled"}")

        if (configuration.cache.isEnabled) {
            cacheLoader.start(scope, ::onCacheResult)
        }
    }

    private suspend fun initializeDeviceProvider(scope: CoroutineScope) {
        logger.debug("Initializing device provider")
        deviceProvider.initialize()

        scope.launch(CoroutineName("device-events-collector")) {
            deviceProvider.deviceEvents
                .filter { isAllowedByConfiguration(it.device) }
                .collect(::onDeviceEvent)
        }
    }

    private suspend fun onCacheResult(cacheResult: CacheResult) {
        when (cacheResult) {
            is CacheResult.Miss -> pools.getValue(cacheResult.pool).send(FromScheduler.AddTests(TestShard(listOf(cacheResult.test))))
            is CacheResult.Hit -> cachedTestsReporter.onCachedTest(cacheResult.pool, cacheResult.testResult)
        }
    }

    private suspend fun onDeviceEvent(event: DeviceEvent) {
        when (event) {
            is DeviceEvent.DeviceConnected -> onDeviceConnected(event.device, coroutineContext)
            is DeviceEvent.DeviceDisconnected -> onDeviceDisconnected(event.device)
        }
    }

    private suspend fun onDeviceDisconnected(device: Device) {
        logger.debug("[{}] Disconnected", device.serialNumber)
        pools.values.forEach {
            it.send(RemoveDevice(device))
        }
    }

    private suspend fun onDeviceConnected(device: Device, context: CoroutineContext) {
        val poolId = poolingStrategy.associate(device)
        logger.debug("[{}] Associated with pool {}", device.serialNumber, poolId)
        pools.computeIfAbsent(poolId) { id ->
            logger.debug("Creating pool actor {}", id)
            DevicePoolActor(id, configuration, analytics, progressReporter, track, timer, logsProvider, strictRunChecker, job, context)
        }
        pools[poolId]?.send(AddDevice(device))
            ?: logger.debug("[{}] Not sending AddDevice event to device pool {}", device.serialNumber, poolId)
        track.deviceConnected(poolId, device.toDeviceInfo())
    }

    private fun isAllowedByConfiguration(device: Device): Boolean {
        val whiteListAccepted = when {
            configuration.includeSerialRegexes.isEmpty() -> true
            else -> configuration.includeSerialRegexes.any { it.matches(device.serialNumber) }
        }
        val blacklistAccepted = when {
            configuration.excludeSerialRegexes.isEmpty() -> true
            else -> configuration.excludeSerialRegexes.none { it.matches(device.serialNumber) }
        }

        return whiteListAccepted && blacklistAccepted
    }
}
