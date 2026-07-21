package com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.stubTestResults
import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FixedQuotaRetryStrategyTest {
    private val devicePoolId = DevicePoolId("DevicePoolId-1")

    @Test
    fun `total quota is 1`() {
        val strategy = FixedQuotaRetryStrategy(totalAllowedRetryQuota = 1)
        val tests = stubTests(10)
        val testResults = stubTestResults(tests)
        val results = strategy.process(devicePoolId, testResults, flakyTests = emptyList())

        assertThat(results).hasSize(1)
    }

    @Test
    fun `total quota more than size of the input list`() {
        val strategy = FixedQuotaRetryStrategy(totalAllowedRetryQuota = 10 + 1)
        val tests = stubTests(10)
        val testResults = stubTestResults(tests)
        val results = strategy.process(devicePoolId, testResults, flakyTests = emptyList())

        assertThat(results).hasSize(10)
    }

    @Test
    fun `should return all tests if flakytests size = 0`() {
        val strategy = FixedQuotaRetryStrategy()
        val tests = stubTests(50)
        val testResults = stubTestResults(tests)
        val results = strategy.process(devicePoolId, testResults, flakyTests = emptyList())

        assertThat(results).hasSize(50)
    }

    @Test
    fun `should return 0 tests if flakiness strategy added 3 flaky tests per test`() {
        val strategy = FixedQuotaRetryStrategy()
        val tests = stubTests(50)
        val testResults = stubTestResults(tests)
        val results = strategy.process(devicePoolId, testResults, flakyTests = tests + tests + tests)

        assertThat(results).isEmpty()
    }
}
