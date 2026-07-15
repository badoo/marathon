package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.exceptions.ConfigurationException
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.flakiness.ProbabilityBasedFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.CountShardingStrategy
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.stopKoin
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import com.malinskiy.marathon.test.Test as MarathonTest

class InvalidConfigTest {
    @AfterEach
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `one healthy device with invalid config should fail`() = runTest {
        val device = StubDevice()
        val test = stubTest("test1")
        val marathon = setupMarathon {
            configuration {
                tests {
                    listOf(test)
                }

                flakinessStrategy = ProbabilityBasedFlakinessStrategy(minSuccessRate = .2, maxCount = 2, timeLimit = Instant.now())
                shardingStrategy = CountShardingStrategy(2)

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device))
                }
            }

            device.executionResults = mapOf(
                test to arrayOf(TestStatus.PASSED)
            )
        }

        assertThrows<ConfigurationException> { marathon.runAsync() }
    }

    private fun stubTest(method: String) =
        MarathonTest(
            pkg = "test",
            clazz = "SimpleTest",
            method = method,
            metaProperties = emptySet(),
            componentInfo = TestComponentInfo()
        )
}
