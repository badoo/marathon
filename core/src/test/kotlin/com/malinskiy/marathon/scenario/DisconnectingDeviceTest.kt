package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.assert.assertJsonEquals
import com.malinskiy.marathon.test.runAsync
import com.malinskiy.marathon.test.setupMarathon
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DisconnectingDeviceTest {
    @Test
    fun `two healthy devices executing two tests while one device disconnects should pass`() = runTest {
        var output: File? = null
        val timerStub = mock<Timer>()

        val device1 = StubDevice(serialNumber = "serial-1")
        val device2 = StubDevice(serialNumber = "serial-2")
        val test1 = stubTest(method = "test1")
        val test2 = stubTest(method = "test2")
        val marathon = setupMarathon {
            configuration {
                timer = timerStub
                output = outputDir

                tests {
                    listOf(test1, test2)
                }

                deviceProviderScope(backgroundScope)

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                    delay(100.milliseconds)
                    it.send(DeviceEvent.DeviceConnected(device2))
                    delay(5.seconds)
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

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/disconnecting_scenario_1.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }
}
