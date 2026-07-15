package com.malinskiy.marathon.execution.strategy.impl.retry

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.generateTestResults
import com.malinskiy.marathon.generateTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoRetryStrategyTest {
    @Test
    fun `should return empty list`() {
        val devicePoolId = DevicePoolId("devicePoolId")
        val tests = generateTests(50)
        val testResults = generateTestResults(tests)
        val strategy = NoRetryStrategy()
        val result = strategy.process(devicePoolId, testResults, flakyTests = emptyList())

        assertThat(result).isEmpty()
    }
}
