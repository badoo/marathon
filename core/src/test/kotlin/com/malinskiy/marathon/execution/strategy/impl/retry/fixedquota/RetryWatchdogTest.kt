package com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RetryWatchdogTest {
    @ParameterizedTest(name = "should return {3} if total allowed quota is {0}, max retry per test quota is {1} and input is {2}")
    @CsvSource(
        "0, 3, 1, false",
        "100, 3, 1, true",
        "100, 0, 1, false",
        "100, 2, 3, false",
        "100, 3, 1, true",
        "100, 2, 1, true"
    )
    fun `should honor total and per-test retry quotas`(
        totalAllowedRetryQuota: Int,
        maxRetryPerTestQuota: Int,
        input: Int,
        expected: Boolean
    ) {
        val watchdog = RetryWatchdog(totalAllowedRetryQuota, maxRetryPerTestQuota)
        val result = watchdog.requestRetry(input)

        assertThat(result).isEqualTo(expected)
    }
}
