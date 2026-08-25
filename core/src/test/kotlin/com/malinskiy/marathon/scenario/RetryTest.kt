package com.malinskiy.marathon.scenario

import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota.FixedQuotaRetryStrategy
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
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RetryTest {
    @Test
    fun `one healthy device with a test that fails then passes on retry should report it as passed`() = runTest {
        var output: File? = null

        val device = StubDevice(serialNumber = "serial-1")
        val test = stubTest(method = "test1")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test)
                }

                retryStrategy = FixedQuotaRetryStrategy(totalAllowedRetryQuota = 100, retryPerTestQuota = 3)

                deviceProviderScope(backgroundScope)

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device))
                }
            }

            device.executionResults = mapOf(
                test to arrayOf(TestStatus.FAILURE, TestStatus.PASSED),
            )
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(20.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/flaky_retry_pass.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }

    @Test
    fun `one healthy device with a test that fails all retries should report it as failed`() = runTest {
        var output: File? = null

        val device = StubDevice(serialNumber = "serial-1")
        val test = stubTest(method = "test1")
        val marathon = setupMarathon {
            configuration {
                output = outputDir

                tests {
                    listOf(test)
                }

                retryStrategy = FixedQuotaRetryStrategy(totalAllowedRetryQuota = 100, retryPerTestQuota = 3)

                deviceProviderScope(backgroundScope)

                devices {
                    delay(1.seconds)
                    it.send(DeviceEvent.DeviceConnected(device))
                }
            }

            device.executionResults = mapOf(
                test to Array(4) { TestStatus.FAILURE },
            )
        }

        val job = launch {
            marathon.runAsync()
        }

        advanceTimeBy(60.seconds)
        val actualReport = File(output!!.absolutePath + "/test_result", "raw.json")
        val expectedReport = File(javaClass.getResource("/output/raw/flaky_retry_failed.json").file)

        assertThat(job.isCompleted).isTrue()
        actualReport.assertJsonEquals(expectedReport)
    }
}
