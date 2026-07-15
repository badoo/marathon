package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota.FixedQuotaRetryStrategy
import com.malinskiy.marathon.test.StubDevice
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import kotlin.time.Duration.Companion.seconds
import com.malinskiy.marathon.test.Test as MarathonTest

@OptIn(ExperimentalCoroutinesApi::class)
class UncompletedRunTest {
    @Test
    fun `one device that never completes tests with 100 uncompleted tests executed should return`() = runTest {
        var output: File? = null

        val device1 = StubDevice(serialNumber = "serial-1")
        val test1 = stubTest("test1")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test1)
                }

                uncompletedTestRetryQuota = 100

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                }
            }

            device1.executionResults = mapOf(test1 to Array(101) { TestStatus.INCOMPLETE })
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(600.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/uncompleted_scenario_1.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }

    @Test
    fun `one device that never completes tests with 100 uncompleted tests while throwing exception should return`() = runTest {
        var output: File? = null
        val timerMock = mock<Timer>()

        val device1 = StubDevice(serialNumber = "serial-1", crashWithTestBatchException = true)
        val test1 = stubTest("test1")
        val marathon = setupMarathon {
            configuration {
                output = outputDir
                timer = timerMock

                tests {
                    listOf(test1)
                }

                uncompletedTestRetryQuota = 100

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
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

        advanceTimeBy(600.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/uncompleted_scenario_1.json").file)

        assertThat(job.isCompleted).isTrue()

        /**
         * Since there are no guarantees about vendor modules reporting failed tests we need to restart the whole batch
         * but then there is a chance that after all the retry quota is exhausted it still didn't finish
         * To mitigate this please report your uncompleted tests
         */
        actualReport.assertJsonEquals(expectedReport)
    }

    @Test
    fun `one device that never completes tests after all retries should report test as failed`() = runTest {
        var output: File? = null

        val device1 = StubDevice(serialNumber = "serial-1")
        val test1 = stubTest("test1")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test1)
                }

                uncompletedTestRetryQuota = 3

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                }
            }

            device1.executionResults = mapOf(test1 to Array(4) { TestStatus.INCOMPLETE })
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(600.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/uncompleted_scenario_2.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }

    @Test
    fun `one device that never completes tests after all retries with retry strategy should report test as failed`() = runTest {
        var output: File? = null

        val device1 = StubDevice(serialNumber = "serial-1")
        val test1 = stubTest("test1")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test1)
                }

                uncompletedTestRetryQuota = 3
                retryStrategy = FixedQuotaRetryStrategy(10, 3)

                vendorConfiguration.deviceProvider.coroutineScope = this@runTest

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device1))
                }
            }

            device1.executionResults = mapOf(test1 to Array(100) { TestStatus.INCOMPLETE })
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(600.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/uncompleted_scenario_2.json").file)

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
