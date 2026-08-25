package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.cache.test.CacheResult.Hit
import com.malinskiy.marathon.cache.test.CacheResult.Miss
import com.malinskiy.marathon.cache.test.key.TestCacheKeyFactory
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.StrictRunConfiguration
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.matches
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.measureTimedValue

class TestCacheLoader(
    private val cache: TestResultsCache,
    private val cacheKeyFactory: TestCacheKeyFactory,
    private val strictRunConfiguration: StrictRunConfiguration,
    private val fetchConcurrency: Int = DEFAULT_FETCH_CONCURRENCY,
) : AutoCloseable {

    private val logger = MarathonLogging.getLogger(TestCacheLoader::class.java)
    private val testsToCheck: Channel<TestToCheck> = unboundedChannel()
    private var job: Job? = null

    init {
        require(fetchConcurrency > 0) { "fetchConcurrency must be positive, got $fetchConcurrency" }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope, consumer: suspend (CacheResult) -> Unit) {
        job = scope.launch(CoroutineName("test-cache-loader")) {
            testsToCheck.consumeAsFlow()
                .flatMapMerge(concurrency = fetchConcurrency) { test ->
                    flow { emit(checkAndLog(test)) }
                }
                // flatMapMerge funnels into this single collector: the consumer must never be invoked concurrently
                .collect(consumer)
        }
    }

    private suspend fun checkAndLog(test: TestToCheck): CacheResult {
        val (result, duration) = measureTimedValue {
            check(test)
        }
        val hitOrMiss = when (result) {
            is Hit -> "hit"
            is Miss -> "miss"
        }
        logger.debug("Cache {} for {} took {}ms", hitOrMiss, test.test.toSimpleSafeTestName(), duration.inWholeMilliseconds)
        return result
    }

    private suspend fun check(test: TestToCheck): CacheResult {
        val strictRun = strictRunConfiguration.filter.matches(test.test)
        if (strictRun) {
            logger.debug("Cache miss for test in blacklist: {}", test.test.toSimpleSafeTestName())
            return Miss(test.poolId, test.test)
        }

        return try {
            val cacheKey = cacheKeyFactory.getCacheKey(test.poolId, test.test)
            cache.load(cacheKey, test.test)
                ?.let { Hit(test.poolId, it) }
                ?: Miss(test.poolId, test.test)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.warn("Error during cache check for {}, falling back to cache miss", test.test.toSimpleSafeTestName(), e)
            Miss(test.poolId, test.test)
        }
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

    private data class TestToCheck(
        val poolId: DevicePoolId,
        val test: Test,
    )

    companion object {
        internal const val DEFAULT_FETCH_CONCURRENCY = 32
    }
}
