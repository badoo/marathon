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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.measureTimedValue

class TestCacheLoader(
    private val configuration: Configuration,
    private val cache: TestResultsCache,
    private val cacheKeyFactory: TestCacheKeyFactory
) {

    private val logger = MarathonLogging.logger("TestCacheLoader")

    private val _results: Channel<CacheResult> = unboundedChannel()
    val results: ReceiveChannel<CacheResult>
        get() = _results

    private val testsToCheck: Channel<TestToCheck> = unboundedChannel()

    private lateinit var cacheCheckCompleted: Deferred<Unit>

    fun initialize(scope: CoroutineScope) = with(scope) {
        cacheCheckCompleted = async {
            testsToCheck.receiveAsFlow()
                .concurrentMap(configuration.cache.concurrency) { test ->
                    val (result, duration) = measureTimedValue { loadFromCache(test) }
                    _results.send(result)

                    val hitOrMiss = if (result is Hit) "hit" else "miss"
                    logger.debug {
                        "Cache $hitOrMiss for ${test.test.toSimpleSafeTestName()}, took ${duration.inWholeMilliseconds} milliseconds"
                    }
                }
                .collect()
        }
    }

    suspend fun addTests(poolId: DevicePoolId, tests: TestShard) {
        if (configuration.cache.isEnabled) {
            val testCacheBlackList: MutableList<Test> = arrayListOf()
            tests.tests.forEach { test ->
                if (configuration.strictRunConfiguration.filter.matches(test)) {
                    testCacheBlackList.add(test)
                } else {
                    testsToCheck.send(TestToCheck(poolId, test))
                }
            }

            if (testCacheBlackList.isNotEmpty()) {
                logger.debug { "Cache miss for test in blacklist: ${testCacheBlackList.map { it.toSimpleSafeTestName() }} " }
                _results.send(Miss(poolId, TestShard(testCacheBlackList)))
            }
        } else {
            _results.send(Miss(poolId, tests))
        }
    }

    suspend fun stop() {
        testsToCheck.close()
        cacheCheckCompleted.await()
        _results.close()
        logger.debug { "Cache loader is terminated" }
    }

    private suspend fun loadFromCache(test: TestToCheck): CacheResult {
        val cacheKey = cacheKeyFactory.getCacheKey(test.poolId, test.test)
        return cache.load(cacheKey, test.test)?.let {
            Hit(test.poolId, it)
        } ?: Miss(test.poolId, TestShard(listOf(test.test)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T, R> Flow<T>.concurrentMap(concurrency: Int, transform: suspend (T) -> R): Flow<R> =
        flatMapMerge(concurrency) { value ->
            flow { emit(transform(value)) }
        }

    private data class TestToCheck(
        val poolId: DevicePoolId,
        val test: Test
    )
}
