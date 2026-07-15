package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.cache.test.CacheResult.Hit
import com.malinskiy.marathon.cache.test.CacheResult.Miss
import com.malinskiy.marathon.cache.test.key.TestCacheKeyFactory
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.matches
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.measureTimedValue

class TestCacheLoader(
    private val configuration: Configuration,
    private val cache: TestResultsCache,
    private val cacheKeyFactory: TestCacheKeyFactory
) : AutoCloseable {

    private val logger = MarathonLogging.getLogger(TestCacheLoader::class.java)
    private val testsToCheck: Channel<TestToCheck> = unboundedChannel()
    private var job: Job? = null

    fun start(scope: CoroutineScope, consumer: suspend (CacheResult) -> Unit) {
        job = scope.launch(CoroutineName("test-cache-loader")) {
            testsToCheck.consumeAsFlow()
                .map { test ->
                    val (result, duration) = measureTimedValue {
                        check(test)
                    }
                    val hitOrMiss = when (result) {
                        is Hit -> "hit"
                        is Miss -> "miss"
                    }
                    logger.debug("Cache {} for {} took {}ms", hitOrMiss, test.test.toSimpleSafeTestName(), duration.inWholeMilliseconds)
                    result
                }
                .collect(consumer)
        }
    }

    private suspend fun check(test: TestToCheck): CacheResult {
        val strictRun = configuration.strictRunConfiguration.filter.matches(test.test)
        if (strictRun) {
            logger.debug("Cache miss for test in blacklist: {}", test.test.toSimpleSafeTestName())
            return Miss(test.poolId, test.test)
        }

        val cacheKey = cacheKeyFactory.getCacheKey(test.poolId, test.test)
        return cache.load(cacheKey, test.test)
            ?.let { Hit(test.poolId, it) }
            ?: Miss(test.poolId, test.test)
    }

    suspend fun addTests(poolId: DevicePoolId, testShard: TestShard) {
        testShard.tests.forEach { test ->
            testsToCheck.send(TestToCheck(poolId, test))
        }
    }

    suspend fun stop() {
        testsToCheck.close()
        job?.join()
        logger.debug("Cache loader is terminated")
    }

    override fun close() {
        testsToCheck.close()
    }

    private data class TestToCheck(val poolId: DevicePoolId, val test: Test)
}
