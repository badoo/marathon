package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.cache.test.key.TestCacheKeyFactory
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.log.MarathonLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TestCacheSaver(
    private val cache: TestResultsCache,
    private val testCacheKeyProvider: TestCacheKeyFactory
) : AutoCloseable {

    private val logger = MarathonLogging.getLogger(TestCacheSaver::class.java)
    private val job = SupervisorJob()
    private val dispatcher = Dispatchers.IO.limitedParallelism(16, "Cache saver")
    private val scope = CoroutineScope(job + dispatcher)

    fun saveTestResult(poolId: DevicePoolId, result: TestResult) {
        scope.launch {
            val cacheKey = testCacheKeyProvider.getCacheKey(poolId, result.test)
            cache.store(cacheKey, result)
        }
    }

    suspend fun terminate() {
        job.complete()
        job.join()
        logger.debug("Cache saver is terminated")
    }

    override fun close() {
        scope.cancel()
    }
}
