package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.execution.StubComponentInfo
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.assert.assertJsonEquals
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class MultipleComponentsTest {
    @Test
    fun `two components scheduled on one device should both run on the shared pool`() = runTest {
        var output: File? = null

        val device = StubDevice(serialNumber = "serial-1")
        val componentA = StubComponentInfo(name = "module-a")
        val componentB = StubComponentInfo(name = "module-b")
        val testA = stubTest(method = "testA", componentInfo = componentA)
        val testB = stubTest(method = "testB", componentInfo = componentB)
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(testA, testB)
                }

                deviceProviderScope(backgroundScope)

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device))
                }
            }

            device.executionResults = mapOf(
                testA to arrayOf(TestStatus.PASSED),
                testB to arrayOf(TestStatus.PASSED)
            )
        }

        val job = launch {
            marathon.runAsync(componentA, componentB)
        }

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/multiple_components.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }

    @Test
    fun `two components scheduled on two devices should run across the shared pool`() = runTest {
        var output: File? = null

        val device1 = StubDevice(serialNumber = "serial-1")
        val device2 = StubDevice(serialNumber = "serial-2")
        val componentA = StubComponentInfo(name = "module-a")
        val componentB = StubComponentInfo(name = "module-b")
        val testA = stubTest(method = "testA", componentInfo = componentA)
        val testB = stubTest(method = "testB", componentInfo = componentB)
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(testA, testB)
                }

                deviceProviderScope(backgroundScope)

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                    delay(100.milliseconds)
                    it.send(DeviceEvent.DeviceConnected(device2))
                }
            }

            device1.executionResults = mapOf(
                testA to arrayOf(TestStatus.PASSED),
                testB to arrayOf(TestStatus.PASSED)
            )
            device2.executionResults = mapOf(
                testA to arrayOf(TestStatus.PASSED),
                testB to arrayOf(TestStatus.PASSED)
            )
        }

        val job = launch {
            marathon.runAsync(componentA, componentB)
        }

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/multiple_components_two_devices.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }
}
