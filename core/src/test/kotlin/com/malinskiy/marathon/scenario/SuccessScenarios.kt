package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.assert.shouldBeEqualToAsJson
import com.malinskiy.marathon.test.setupMarathon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.amshove.kluent.shouldBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.koin.core.context.stopKoin
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SuccessScenarios : Spek(
    {
        afterEachTest {
            stopKoin()
        }

        given("one healthy device") {
            on("execution of one test") {
                it("should pass") {
                    var output: File? = null
                    val coroutineScope = TestCoroutineScope()

                    val marathon = setupMarathon {
                        val test = Test("test", "SimpleTest", "test", emptySet(), TestComponentInfo())
                        val device = StubDevice()

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test)
                            }

                            vendorConfiguration.deviceProvider.coroutineScope = coroutineScope

                            devices {
                                delay(1000)
                                it.send(DeviceProvider.DeviceEvent.DeviceConnected(device))
                            }
                        }

                        device.executionResults = mapOf(
                            test to arrayOf(TestStatus.PASSED)
                        )
                    }

                    val job = coroutineScope.launch {
                        marathon.runAsync()
                    }

                    coroutineScope.advanceTimeBy(TimeUnit.SECONDS.toMillis(20))

                    job.isCompleted shouldBe true
                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .shouldBeEqualToAsJson(File(javaClass.getResource("/output/raw/success_scenario_1.json").file))
                }
            }
        }
    })
