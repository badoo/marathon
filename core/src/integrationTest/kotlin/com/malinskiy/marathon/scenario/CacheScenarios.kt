package com.malinskiy.marathon.scenario

import com.google.gson.JsonParser
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.cache.gradle.GradleCacheContainer
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.CacheConfiguration
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import com.malinskiy.marathon.test.toTestName
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.stopKoin
import java.io.File
import com.malinskiy.marathon.test.Test as MarathonTest

class CacheScenarios {
    @AutoClose
    private val container = GradleCacheContainer()

    @TempDir
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        container.start()
    }

    @Test
    fun `GIVEN cache is enabled and empty WHEN running tests first time THEN tests gets executed`() = runTest {
        val test = createTest()
        val outputDir = tempDir.resolve("build-1")

        runMarathonWithOneTest(
            cacheConfig = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl)),
            outputDir = outputDir,
            test = test
        )

        val isFromCache = isFromCache(outputDir, test)
        assertFalse(isFromCache)
    }

    @Test
    fun `GIVEN cache is enabled WHEN running tests second time THEN test results get taken from cache`() = runTest {
        val test = createTest()
        val cacheConfiguration = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl))

        val build1OutputDir = tempDir.resolve("build-1")
        runMarathonWithOneTest(cacheConfiguration, build1OutputDir, test)

        val build2OutputDir = tempDir.resolve("build-2")
        runMarathonWithOneTest(cacheConfiguration, build2OutputDir, test)

        val isFromCache = isFromCache(build2OutputDir, test)
        assertTrue(isFromCache)
    }

    private fun createTest(): MarathonTest =
        MarathonTest(
            pkg = "test",
            clazz = "SimpleTest",
            method = "test",
            metaProperties = emptySet(),
            componentInfo = TestComponentInfo()
        )

    private fun isFromCache(outputDir: File, test: MarathonTest): Boolean {
        val testResultJson = outputDir.resolve("test_result/omni/serial-1/${test.toTestName()}.json")
        return testResultJson.reader().use { JsonParser.parseReader(it).asJsonObject.get("isFromCache").asBoolean }
    }

    private suspend fun TestScope.runMarathonWithOneTest(
        cacheConfig: CacheConfiguration,
        outputDir: File,
        test: MarathonTest
    ) {
        val marathon = setupMarathon {
            val device = StubDevice()

            configuration {
                this.outputDir = outputDir

                tests {
                    listOf(test)
                }

                cache = cacheConfig
                vendorConfiguration.deviceProvider.coroutineScope = this@runMarathonWithOneTest

                devices {
                    delay(1000)
                    it.send(DeviceProvider.DeviceEvent.DeviceConnected(device))
                }
            }

            device.executionResults = mapOf(
                test to arrayOf(TestStatus.PASSED)
            )
        }

        marathon.runAsync()
        stopKoin()
    }
}
