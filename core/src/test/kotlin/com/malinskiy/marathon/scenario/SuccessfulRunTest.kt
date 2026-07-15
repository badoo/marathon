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
class SuccessfulRunTest {
    @AfterEach
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `one healthy device executing one test should pass`() = runTest {
        var output: File? = null

        val device = StubDevice()
        val test = stubTest("test")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test)
                }

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

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/success_scenario_1.json").file)

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
