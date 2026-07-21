package com.malinskiy.marathon.analytics.metrics

import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.test.stubTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class NoOpMetricsProviderTest {
    private val metricsProvider = NoOpMetricsProvider()

    @Test
    fun shouldReturn0AsSuccessRate() {
        val test = stubTest()
        val successRate = metricsProvider.successRate(test, Instant.now())
        assertThat(successRate).isEqualTo(0.0)
    }

    @Test
    fun shouldReturn0AsExecutionTime() {
        val test = stubTest()
        val executionTime = metricsProvider.executionTime(test, 90.0, Instant.now())
        assertThat(executionTime).isEqualTo(0.0)
    }
}
