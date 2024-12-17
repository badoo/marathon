package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota.FixedQuotaRetryStrategy
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.assert.assertJsonEquals
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertTrue
import org.koin.core.context.stopKoin
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class UncompletedScenarios : Spek({
    afterEachTest {
        stopKoin()
    }

    describe("one device that never completes tests") {
        group("100 uncompleted tests executed") {
            it("should return") {
                runTest {
                    var output: File? = null

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 100

                            vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                            devices {
                                delay(1000)
                                it.send(DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(101) { TestStatus.INCOMPLETE })
                    }

                    val job = launch {
                        marathon.runAsync()
                    }

                    advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    assertTrue(job.isCompleted)

                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .assertJsonEquals(File(javaClass.getResource("/output/raw/uncompleted_scenario_1.json").file))
                }
            }
        }

        group("100 uncompleted tests while throwing exception") {
            it("should return") {
                runTest {
                    var output: File? = null
                    val timerMock = mock<Timer>()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1", crashWithTestBatchException = true)

                        configuration {
                            output = outputDir
                            timer = timerMock

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 100

                            vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                            devices {
                                delay(1000)
                                it.send(DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(101) { TestStatus.INCOMPLETE })
                    }

                    var i = 0L
                    whenever(timerMock.currentTimeMillis()).then { i++ }

                    val job = launch {
                        marathon.runAsync()
                    }

                    advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    assertTrue(job.isCompleted)

                    /**
                     * Since there are no guarantees about vendor modules reporting failed tests we need to restart the whole batch
                     * but then there is a chance that after all the retry quota is exhausted it still didn't finish
                     * To mitigate this please report your uncompleted tests
                     */
                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .assertJsonEquals(File(javaClass.getResource("/output/raw/uncompleted_scenario_1.json").file))
                }
            }
        }

        group("one device that never completes tests after all retries") {
            it("should report test as failed") {
                runTest {
                    var output: File? = null

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 3

                            vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                            devices {
                                delay(1000)
                                it.send(DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(4) { TestStatus.INCOMPLETE })
                    }

                    val job = launch {
                        marathon.runAsync()
                    }

                    advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    assertTrue(job.isCompleted)

                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .assertJsonEquals(File(javaClass.getResource("/output/raw/uncompleted_scenario_2.json").file))
                }
            }
        }

        group("one device that never completes tests after all retries with retry strategy") {
            it("should report test as failed") {
                runTest {
                    var output: File? = null

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1")

                        configuration {
                            output = outputDir

                            tests {
                                listOf(test1)
                            }

                            uncompletedTestRetryQuota = 3
                            retryStrategy = FixedQuotaRetryStrategy(10, 3)

                            vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                            devices {
                                delay(1000)
                                it.send(DeviceEvent.DeviceConnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(test1 to Array(100) { TestStatus.INCOMPLETE })
                    }

                    val job = launch {
                        marathon.runAsync()
                    }

                    advanceTimeBy(TimeUnit.SECONDS.toMillis(600))

                    assertTrue(job.isCompleted)

                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .assertJsonEquals(File(javaClass.getResource("/output/raw/uncompleted_scenario_2.json").file))
                }
            }
        }
    }
})
