package com.malinskiy.marathon.scenario

import com.google.gson.JsonParser
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.cache.gradle.GradleCacheContainer
import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.execution.CacheConfiguration
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.test.toTestName
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import kotlin.time.Duration.Companion.seconds
import com.malinskiy.marathon.test.Test as MarathonTest

@Testcontainers
class CacheTest {
    @Container
    private val container = GradleCacheContainer()

    @TempDir
    private lateinit var tempDir: File

    @Test
    fun `GIVEN cache is enabled and empty WHEN running tests first time THEN tests gets executed`() = runTest {
        val test = stubTest(method = "test")
        val outputDir = tempDir.resolve("build-1")

        runMarathonWithOneTest(
            cacheConfig = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl)),
            outputDir = outputDir,
            test = test
        )

        val isFromCache = isFromCache(outputDir, test)

        assertThat(isFromCache).isFalse()
    }

    @Test
    fun `GIVEN cache is enabled WHEN running tests second time THEN test results get taken from cache`() = runTest {
        val test = stubTest(method = "test")
        val cacheConfiguration = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl))

        val build1OutputDir = tempDir.resolve("build-1")
        runMarathonWithOneTest(cacheConfiguration, build1OutputDir, test)

        val build2OutputDir = tempDir.resolve("build-2")
        runMarathonWithOneTest(cacheConfiguration, build2OutputDir, test)

        val isFromCache = isFromCache(build2OutputDir, test)

        assertThat(isFromCache).isTrue()
    }

    @Test
    fun `GIVEN cache is enabled and push is disabled WHEN running tests second time THEN test results are not from cache`() = runTest {
        val test = stubTest(method = "test")
        val cacheConfiguration = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl, push = false))

        val build1OutputDir = tempDir.resolve("build-1")
        runMarathonWithOneTest(cacheConfiguration, build1OutputDir, test)

        val build2OutputDir = tempDir.resolve("build-2")
        runMarathonWithOneTest(cacheConfiguration, build2OutputDir, test)

        val isFromCache = isFromCache(build2OutputDir, test)

        assertThat(isFromCache).isFalse()
    }

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
                deviceProviderScope(this@runMarathonWithOneTest)

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device))
                }
            }

            device.executionResults = mapOf(
                test to arrayOf(TestStatus.PASSED)
            )
        }

        marathon.runAsync()
    }
}
