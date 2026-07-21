package com.malinskiy.marathon.execution.strategy.impl.retry

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.stubTestResults
import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoRetryStrategyTest {
    @Test
    fun `should return empty list`() {
        val devicePoolId = DevicePoolId("devicePoolId")
        val tests = stubTests(50)
        val testResults = stubTestResults(tests)
        val strategy = NoRetryStrategy()
        val result = strategy.process(devicePoolId, testResults, flakyTests = emptyList())

        assertThat(result).isEmpty()
    }
}
