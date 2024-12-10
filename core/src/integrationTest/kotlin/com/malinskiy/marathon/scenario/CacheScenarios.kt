package com.malinskiy.marathon.scenario

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.cache.gradle.GradleCacheContainer
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.CacheConfiguration
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.TestBody
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.koin.core.context.stopKoin
import java.io.File

class CacheScenarios : Spek({
    val container = GradleCacheContainer()

    beforeGroup {
        container.start()
    }

    afterGroup {
        container.stop()
    }

    given("cache is enabled") {
        group("the first execution of the test") {
            it("should execute the test") {
                val outputDir = runMarathonWithOneTest(
                    test = Test("test", "ExampleTest", "test", emptySet(), TestComponentInfo()),
                    cacheConfig = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl))
                )

                val isFromCache = File(outputDir.absolutePath + "/test_result/omni/serial-1", "test.ExampleTest#test.json")
                    .jsonObject
                    .get("isFromCache")
                    .asBoolean
                isFromCache shouldBeEqualTo false
            }
        }

        group("the second execution of the test") {
            it("should restored the test from cache") {
                runMarathonWithOneTest(
                    test = Test("test", "SimpleTest", "test", emptySet(), TestComponentInfo()),
                    cacheConfig = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl))
                )
                val secondRunDir = runMarathonWithOneTest(
                    test = Test("test", "SimpleTest", "test", emptySet(), TestComponentInfo()),
                    cacheConfig = CacheConfiguration(remote = RemoteCacheConfiguration.Enabled(url = container.cacheUrl))
                )

                val isFromCache = File(secondRunDir.absolutePath + "/test_result/omni/serial-1", "test.SimpleTest#test.json")
                    .jsonObject
                    .get("isFromCache")
                    .asBoolean
                isFromCache shouldBeEqualTo true
            }
        }
    }
})

private val File.jsonObject: JsonObject
    get() = JsonParser.parseReader(reader()).asJsonObject

private fun TestBody.runMarathonWithOneTest(
    test: Test,
    cacheConfig: CacheConfiguration
): File = runBlocking {
    var output: File? = null

    val marathon = setupMarathon {
        val device = StubDevice()

        configuration {
            output = outputDir

            tests {
                listOf(test)
            }

            cache = cacheConfig

            vendorConfiguration.deviceProvider.coroutineScope = this@runBlocking

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

    output!!
}
