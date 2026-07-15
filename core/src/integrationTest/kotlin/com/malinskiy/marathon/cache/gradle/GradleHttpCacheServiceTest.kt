package com.malinskiy.marathon.cache.gradle

import com.malinskiy.marathon.cache.SimpleCacheKey
import com.malinskiy.marathon.cache.SimpleEntryReader
import com.malinskiy.marathon.cache.SimpleEntryWriter
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class GradleHttpCacheServiceTest {
    @Container
    private val container = GradleCacheContainer()

    @AutoClose
    private lateinit var cacheService: GradleHttpCacheService

    @BeforeEach
    fun setUp() {
        cacheService = GradleHttpCacheService(RemoteCacheConfiguration.Enabled(container.cacheUrl))
    }

    @Test
    fun `GIVEN empty cache WHEN loading an entry from cache THEN returns false`() = runTest {
        val cacheKey = SimpleCacheKey("this_key_does_not_exists")

        val reader = SimpleEntryReader()
        val result = cacheService.load(cacheKey, reader)

        assertFalse(result)
        assertFalse(reader.readInvoked)
    }

    @Test
    fun `GIVEN cache with an entry WHEN loading the entry from cache THEN returns the original data`() = runTest {
        val cacheKey = SimpleCacheKey("test")
        cacheService.store(cacheKey, SimpleEntryWriter("qwerty"))

        val reader = SimpleEntryReader()
        val result = cacheService.load(cacheKey, reader)

        assertTrue(result)
        assertTrue(reader.readInvoked)
        assertEquals("qwerty", reader.data)
    }
}
