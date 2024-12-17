package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.execution.TestStatus
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
class DisconnectingScenarios : Spek({
    afterEachTest {
        stopKoin()
    }

    describe("two healthy devices") {
        group("execution of two tests while one device disconnects") {
            it("should pass") {
                runTest {
                    var output: File? = null
                    val timerStub = mock<Timer>()

                    val marathon = setupMarathon {
                        val test1 = Test("test", "SimpleTest", "test1", emptySet(), TestComponentInfo())
                        val test2 = Test("test", "SimpleTest", "test2", emptySet(), TestComponentInfo())
                        val device1 = StubDevice(serialNumber = "serial-1")
                        val device2 = StubDevice(serialNumber = "serial-2")

                        configuration {
                            timer = timerStub
                            output = outputDir

                            tests {
                                listOf(test1, test2)
                            }

                            vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                            devices {
                                delay(1000)
                                it.send(DeviceEvent.DeviceConnected(device1))
                                delay(100)
                                it.send(DeviceEvent.DeviceConnected(device2))
                                delay(5000)
                                it.send(DeviceEvent.DeviceDisconnected(device1))
                            }
                        }

                        device1.executionResults = mapOf(
                            test1 to arrayOf(TestStatus.INCOMPLETE),
                            test2 to arrayOf(TestStatus.INCOMPLETE)
                        )
                        device2.executionResults = mapOf(
                            test1 to arrayOf(TestStatus.PASSED),
                            test2 to arrayOf(TestStatus.PASSED)
                        )
                    }

                    var i = 0L
                    whenever(timerStub.currentTimeMillis()).then { i++ }

                    val job = launch {
                        marathon.runAsync()
                    }

                    advanceTimeBy(TimeUnit.SECONDS.toMillis(20))

                    assertTrue(job.isCompleted)

                    File(output!!.absolutePath + "/test_result", "raw.json")
                        .assertJsonEquals(File(javaClass.getResource("/output/raw/disconnecting_scenario_1.json").file))
                }
            }
        }
    }
})
