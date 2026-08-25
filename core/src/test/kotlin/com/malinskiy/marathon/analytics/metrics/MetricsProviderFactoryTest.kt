package com.malinskiy.marathon.analytics.metrics

import com.malinskiy.marathon.analytics.external.MetricsProviderFactory
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetricsProviderFactoryTest {
    @Test
    fun shouldReturnNoopProviderWhenDisabled() {
        val factory = MetricsProviderFactory()
        val metricsProvider = factory.create()
        assertThat(metricsProvider).isInstanceOf(NoOpMetricsProvider::class.java)
    }
}
