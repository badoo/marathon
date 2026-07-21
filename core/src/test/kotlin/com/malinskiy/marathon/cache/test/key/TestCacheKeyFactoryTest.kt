package com.malinskiy.marathon.cache.test.key

import com.malinskiy.marathon.cache.CacheKey
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import com.malinskiy.marathon.test.Test as MarathonTest

class TestCacheKeyFactoryTest {
    @Test
    fun differentCacheKeysForDifferentMarathonVersion() = runTest {
        val firstKey = createCacheKey(marathonVersion = "1.0")
        val secondKey = createCacheKey(marathonVersion = "1.1")

        assertNotEquals(firstKey, secondKey)
    }

    @Test
    fun sameCacheKeysForSameMarathonVersion() = runTest {
        val firstKey = createCacheKey(marathonVersion = "1.0")
        val secondKey = createCacheKey(marathonVersion = "1.0")

        assertEquals(firstKey, secondKey)
    }

    @Test
    fun differCacheKeysForDifferentComponentCacheKeys() = runTest {
        val firstKey = createCacheKey(componentCacheKey = "abc")
        val secondKey = createCacheKey(componentCacheKey = "def")

        assertNotEquals(firstKey, secondKey)
    }

    @Test
    fun sameCacheKeysForSameComponentCacheKeys() = runTest {
        val firstKey = createCacheKey(componentCacheKey = "abc")
        val secondKey = createCacheKey(componentCacheKey = "abc")

        assertEquals(firstKey, secondKey)
    }

    @Test
    fun differentCacheKeysForDifferentDevicePools() = runTest {
        val firstKey = createCacheKey(devicePoolId = DevicePoolId("abc"))
        val secondKey = createCacheKey(devicePoolId = DevicePoolId("def"))

        assertNotEquals(firstKey, secondKey)
    }

    @Test
    fun sameCacheKeysForTheSameDevicePools() = runTest {
        val firstKey = createCacheKey(devicePoolId = DevicePoolId("abc"))
        val secondKey = createCacheKey(devicePoolId = DevicePoolId("abc"))

        assertEquals(firstKey, secondKey)
    }

    @Test
    fun differentCacheKeysForDifferentTestPackageNames() = runTest {
        val firstKey = createCacheKey(test = stubTest(pkg = "abc"))
        val secondKey = createCacheKey(test = stubTest(pkg = "def"))

        assertNotEquals(firstKey, secondKey)
    }

    @Test
    fun sameCacheKeysForTheSamePackageNames() = runTest {
        val firstKey = createCacheKey(test = stubTest(pkg = "abc"))
        val secondKey = createCacheKey(test = stubTest(pkg = "abc"))

        assertEquals(firstKey, secondKey)
    }

    @Test
    fun differentCacheKeysForDifferentClassNames() = runTest {
        val firstKey = createCacheKey(test = stubTest(clazz = "abc"))
        val secondKey = createCacheKey(test = stubTest(clazz = "def"))

        assertNotEquals(firstKey, secondKey)
    }

    @Test
    fun sameCacheKeysForTheSameClassNames() = runTest {
        val firstKey = createCacheKey(test = stubTest(clazz = "abc"))
        val secondKey = createCacheKey(test = stubTest(clazz = "abc"))

        assertEquals(firstKey, secondKey)
    }

    @Test
    fun differentCacheKeysForDifferentMethodNames() = runTest {
        val firstKey = createCacheKey(test = stubTest(method = "abc"))
        val secondKey = createCacheKey(test = stubTest(method = "def"))

        assertNotEquals(firstKey, secondKey)
    }

    @Test
    fun sameCacheKeysForTheSameMethodNames() = runTest {
        val firstKey = createCacheKey(test = stubTest(method = "abc"))
        val secondKey = createCacheKey(test = stubTest(method = "abc"))

        assertEquals(firstKey, secondKey)
    }

    @Test
    fun padsKeyWithLeadingZerosTo32Characters() = runTest {
        val cacheKey = createCacheKey(test = stubTest(method = "method886"))

        assertThat(cacheKey.key).isEqualTo("0017cf2d02916751c1f29bb808b6169c")
    }

    private suspend fun createCacheKey(
        marathonVersion: String = "123",
        componentCacheKey: String = "abc",
        devicePoolId: DevicePoolId = DevicePoolId("omni"),
        test: MarathonTest = stubTest()
    ): CacheKey {
        val componentCacheKeyProvider = ComponentCacheKeyProvider { componentCacheKey }
        val versionNameProvider = mock<VersionNameProvider> {
            on { this.versionName }.thenReturn(marathonVersion)
        }
        val cacheKeyFactory = TestCacheKeyFactory(componentCacheKeyProvider, versionNameProvider)
        return cacheKeyFactory.getCacheKey(devicePoolId, test)
    }
}
