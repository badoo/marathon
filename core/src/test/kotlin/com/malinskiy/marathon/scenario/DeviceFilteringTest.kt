package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.assert.assertJsonEquals
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import java.io.File
import kotlin.time.Duration.Companion.seconds
import com.malinskiy.marathon.test.Test as MarathonTest

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceFilteringTest {
    @AfterEach
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `one blacklisted device and empty whitelist should pass on one device`() = runTest {
        var output: File? = null

        val device1 = StubDevice(serialNumber = "emulator-5000")
        val device2 = StubDevice(serialNumber = "emulator-5002")
        val test1 = stubTest("test1")
        val test2 = stubTest("test2")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test1, test2)
                }

                excludeSerialRegexes = listOf("""emulator-5002""".toRegex())
                includeSerialRegexes = emptyList()

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                    it.send(DeviceEvent.DeviceConnected(device2))
                }
            }

            device1.executionResults = mapOf(
                test1 to arrayOf(TestStatus.PASSED),
                test2 to arrayOf(TestStatus.PASSED)
            )
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/device_filtering_1.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }

    @Test
    fun `one whitelisted device and empty blacklist should pass on one device`() = runTest {
        var output: File? = null

        val device1 = StubDevice(serialNumber = "emulator-5000")
        val device2 = StubDevice(serialNumber = "emulator-5002")
        val test1 = stubTest("test1")
        val test2 = stubTest("test2")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test1, test2)
                }

                excludeSerialRegexes = emptyList()
                includeSerialRegexes = listOf("""emulator-5002""".toRegex())

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                    it.send(DeviceEvent.DeviceConnected(device2))
                }
            }

            device2.executionResults = mapOf(
                test1 to arrayOf(TestStatus.PASSED),
                test2 to arrayOf(TestStatus.PASSED)
            )
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/device_filtering_2.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }

    @Test
    fun `one blacklisted device and one whitelisted should pass on one device`() = runTest {
        var output: File? = null

        val device1 = StubDevice(serialNumber = "emulator-5000")
        val device2 = StubDevice(serialNumber = "emulator-5002")
        val device3 = StubDevice(serialNumber = "emulator-5004")
        val test1 = stubTest("test1")
        val test2 = stubTest("test2")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test1, test2)
                }

                excludeSerialRegexes = listOf("""emulator-5002""".toRegex())
                includeSerialRegexes = listOf("""emulator-500[2,4]""".toRegex())

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                    it.send(DeviceEvent.DeviceConnected(device2))
                    it.send(DeviceEvent.DeviceConnected(device3))
                }
            }

            device3.executionResults = mapOf(
                test1 to arrayOf(TestStatus.PASSED),
                test2 to arrayOf(TestStatus.PASSED)
            )
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/device_filtering_3.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
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
