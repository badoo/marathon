package com.malinskiy.marathon.cache.test.key

import com.malinskiy.marathon.cache.CacheKey
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.factory.configuration
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import com.malinskiy.marathon.test.Test as MarathonTest

class TestCacheKeyFactoryTest {

    @Test
    fun differentCacheKeysForDifferentMarathonVersion() {
        runBlocking {
            val firstKey = createCacheKey(marathonVersion = "1.0")
            val secondKey = createCacheKey(marathonVersion = "1.1")

            firstKey shouldNotBeEqualTo secondKey
        }
    }

    @Test
    fun sameCacheKeysForSameMarathonVersion() {
        runBlocking {
            val firstKey = createCacheKey(marathonVersion = "1.0")
            val secondKey = createCacheKey(marathonVersion = "1.0")

            firstKey shouldBeEqualTo secondKey
        }
    }

    @Test
    fun differCacheKeysForDifferentComponentCacheKeys() {
        runBlocking {
            val firstKey = createCacheKey(componentCacheKey = "abc")
            val secondKey = createCacheKey(componentCacheKey = "def")

            firstKey shouldNotBeEqualTo secondKey
        }
    }

    @Test
    fun sameCacheKeysForSameComponentCacheKeys() {
        runBlocking {
            val firstKey = createCacheKey(componentCacheKey = "abc")
            val secondKey = createCacheKey(componentCacheKey = "abc")

            firstKey shouldBeEqualTo secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentDevicePools() {
        runBlocking {
            val firstKey = createCacheKey(devicePoolId = DevicePoolId("abc"))
            val secondKey = createCacheKey(devicePoolId = DevicePoolId("def"))

            firstKey shouldNotBeEqualTo secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSameDevicePools() {
        runBlocking {
            val firstKey = createCacheKey(devicePoolId = DevicePoolId("abc"))
            val secondKey = createCacheKey(devicePoolId = DevicePoolId("abc"))

            firstKey shouldBeEqualTo secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentCodeCoverageConfigurations() {
        runBlocking {
            val firstKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = true))
            val secondKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = false))

            firstKey shouldNotBeEqualTo secondKey
        }
    }

    @Test
    fun sameCacheKeysForSameCodeCoverageConfigurations() {
        runBlocking {
            val firstKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = true))
            val secondKey = createCacheKey(configuration = createConfiguration(codeCoverageEnabled = true))

            firstKey shouldBeEqualTo secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentTestPackageNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(packageName = "abc"))
            val secondKey = createCacheKey(test = createTest(packageName = "def"))

            firstKey shouldNotBeEqualTo secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSamePackageNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(packageName = "abc"))
            val secondKey = createCacheKey(test = createTest(packageName = "abc"))

            firstKey shouldBeEqualTo secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentClassNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(clazz = "abc"))
            val secondKey = createCacheKey(test = createTest(clazz = "def"))

            firstKey shouldNotBeEqualTo secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSameClassNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(clazz = "abc"))
            val secondKey = createCacheKey(test = createTest(clazz = "abc"))

            firstKey shouldBeEqualTo secondKey
        }
    }

    @Test
    fun differentCacheKeysForDifferentMethodNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(method = "abc"))
            val secondKey = createCacheKey(test = createTest(method = "def"))

            firstKey shouldNotBeEqualTo secondKey
        }
    }

    @Test
    fun sameCacheKeysForTheSameMethodNames() {
        runBlocking {
            val firstKey = createCacheKey(test = createTest(method = "abc"))
            val secondKey = createCacheKey(test = createTest(method = "abc"))

            firstKey shouldBeEqualTo secondKey
        }
    }
}

private fun createCacheKey(
    marathonVersion: String = "123",
    componentCacheKey: String = "abc",
    configuration: Configuration = createConfiguration(),
    devicePoolId: DevicePoolId = DevicePoolId("omni"),
    test: MarathonTest = createTest()
): CacheKey = runBlocking {
    val componentCacheKeyProvider = object : ComponentCacheKeyProvider {
        override suspend fun getCacheKey(componentInfo: ComponentInfo): String = componentCacheKey
    }
    val versionNameProvider = mock<VersionNameProvider> {
        on { this.versionName }.thenReturn(marathonVersion)
    }
    val cacheKeyFactory = TestCacheKeyFactory(componentCacheKeyProvider, versionNameProvider, configuration)
    cacheKeyFactory.getCacheKey(devicePoolId, test)
}

private fun createTest(
    packageName: String = "com.test",
    clazz: String = "Test",
    method: String = "test1"
) = MarathonTest(
    pkg = packageName,
    clazz = clazz,
    method = method,
    componentInfo = TestComponentInfo(someInfo = "someInfo", name = "component-name"),
    metaProperties = emptyList()
)

private fun createConfiguration(codeCoverageEnabled: Boolean = false) = configuration {
    isCodeCoverageEnabled = codeCoverageEnabled
}
