package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.exceptions.ConfigurationException
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.flakiness.ProbabilityBasedFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.CountShardingStrategy
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class InvalidConfigTest {
    @Test
    fun `one healthy device with invalid config should fail`() = runTest {
        val device = StubDevice()
        val test = stubTest(method = "test1")
        val marathon = setupMarathon {
            configuration {
                tests {
                    listOf(test)
                }

                flakinessStrategy = ProbabilityBasedFlakinessStrategy(minSuccessRate = .2, maxCount = 2, timeLimit = Instant.now())
                shardingStrategy = CountShardingStrategy(2)

                deviceProviderScope(backgroundScope)

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
}
