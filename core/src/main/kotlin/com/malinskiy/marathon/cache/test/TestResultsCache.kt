package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.cache.CacheKey
import com.malinskiy.marathon.cache.CacheService
import com.malinskiy.marathon.cache.test.serialization.TestResultEntryReader
import com.malinskiy.marathon.cache.test.serialization.TestResultEntryWriter
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Instant

class TestResultsCache(
    private val cacheService: CacheService,
    private val attachmentManager: AttachmentManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val track: Track
) {

    private val logger = MarathonLogging.getLogger(TestResultsCache::class.java)

    suspend fun load(key: CacheKey, test: Test): TestResult? {
        val start = Instant.now()
        try {
            val reader = TestResultEntryReader(test, attachmentManager, ioDispatcher)
            if (!cacheService.load(key, reader)) {
                return null
            }
            return reader.testResult
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.warn("Error during loading cache entry for {}", test.toSimpleSafeTestName(), e)
            return null
        } finally {
            val finish = Instant.now()
            track.cacheLoad(start, finish, test)
        }
    }

    suspend fun store(key: CacheKey, testResult: TestResult) {
        val start = Instant.now()
        try {
            val writer = TestResultEntryWriter(testResult)
            cacheService.store(key, writer)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            logger.warn("Error during storing cache entry for {}", testResult.test.toSimpleSafeTestName(), e)
        } finally {
            val finish = Instant.now()
            track.cacheStore(start, finish, testResult.test)
        }
    }
}
