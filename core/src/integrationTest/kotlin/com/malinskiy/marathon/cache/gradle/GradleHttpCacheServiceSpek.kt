package com.malinskiy.marathon.cache.gradle

import com.malinskiy.marathon.cache.SimpleCacheKey
import com.malinskiy.marathon.cache.SimpleEntryReader
import com.malinskiy.marathon.cache.SimpleEntryWriter
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import kotlinx.coroutines.test.runTest
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.lifecycle.CachingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class GradleHttpCacheServiceSpek : Spek({
    val container = GradleCacheContainer()

    val cacheService = memoized(mode = CachingMode.TEST) {
        GradleHttpCacheService(RemoteCacheConfiguration.Enabled(container.cacheUrl))
    }

    afterEachTest {
        cacheService().close()
    }

    beforeGroup {
        container.start()
    }

    afterGroup {
        container.stop()
    }

    describe("GradleHttpCacheService") {
        group("basics") {
            it("load with empty cache - should return false") {
                runTest {
                    val reader = SimpleEntryReader()
                    val result = cacheService().load(SimpleCacheKey("this_key_does_not_exists"), reader)

                    assertFalse(result)
                    assertFalse(reader.readInvoked)
                }
            }

            it("save to cache and load - should return the same data") {
                runTest {
                    cacheService().store(SimpleCacheKey("test"), SimpleEntryWriter("qwerty"))
                    val reader = SimpleEntryReader()
                    val result = cacheService().load(SimpleCacheKey("test"), reader)

                    assertTrue(result)
                    assertTrue(reader.readInvoked)
                    assertEquals("qwerty", reader.data)
                }
            }
        }
    }
})
