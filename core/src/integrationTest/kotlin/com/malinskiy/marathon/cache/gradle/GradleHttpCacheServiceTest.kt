package com.malinskiy.marathon.cache.gradle

import com.malinskiy.marathon.cache.CacheService
import com.malinskiy.marathon.cache.SimpleCacheKey
import com.malinskiy.marathon.cache.SimpleEntryReader
import com.malinskiy.marathon.cache.SimpleEntryWriter
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class GradleHttpCacheServiceTest {
    @Container
    private val container = GradleCacheContainer()

    @Test
    fun `GIVEN empty cache WHEN loading an entry from cache THEN returns false`() = runCacheServiceTest { cacheService ->
        val cacheKey = SimpleCacheKey("this_key_does_not_exists")

        val reader = SimpleEntryReader()
        val result = cacheService.load(cacheKey, reader)

        assertThat(result).isFalse()
        assertThat(reader.readInvoked).isFalse()
    }

    @Test
    fun `GIVEN cache with an entry WHEN loading the entry from cache THEN returns the original data`() = runCacheServiceTest { cacheService ->
        val cacheKey = SimpleCacheKey("test")
        cacheService.store(cacheKey, SimpleEntryWriter("qwerty"))

        val reader = SimpleEntryReader()
        val result = cacheService.load(cacheKey, reader)

        assertThat(result).isTrue()
        assertThat(reader.readInvoked).isTrue()
        assertThat(reader.data).isEqualTo("qwerty")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runCacheServiceTest(testBody: suspend (CacheService) -> Unit) = runTest {
        GradleHttpCacheService(
            configuration = RemoteCacheConfiguration.Enabled(container.cacheUrl),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler)
        ).use { testBody(it) }
    }
}
