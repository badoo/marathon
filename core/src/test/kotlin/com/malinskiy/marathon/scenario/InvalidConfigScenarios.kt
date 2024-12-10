package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.exceptions.ConfigurationException
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.flakiness.ProbabilityBasedFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.CountShardingStrategy
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.assertThrows
import org.koin.core.context.stopKoin
import java.time.Instant

class InvalidConfigScenarios : Spek({
    afterEachTest {
        stopKoin()
    }

    describe("one healthy device") {
        group("invalid config") {
            it("should fail") {
                runTest {
                    val marathon = setupMarathon {
                        val test = Test("test", "SimpleTest", "test", emptySet(), TestComponentInfo())
                        val device = StubDevice()

                        configuration {
                            tests {
                                listOf(test)
                            }

                            flakinessStrategy = ProbabilityBasedFlakinessStrategy(.2, 2, Instant.now())
                            shardingStrategy = CountShardingStrategy(2)

                            vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device))
                            }
                        }

                        device.executionResults = mapOf(
                            test to arrayOf(TestStatus.PASSED)
                        )
                    }

                    assertThrows<ConfigurationException> {
                        marathon.runAsync()
                    }
                }
            }
        }
    }
})
