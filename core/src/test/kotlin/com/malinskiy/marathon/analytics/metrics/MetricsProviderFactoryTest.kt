package com.malinskiy.marathon.analytics.metrics

import com.malinskiy.marathon.analytics.external.MetricsProviderFactory
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class MetricsProviderFactoryTest {

    @Test
    fun shouldReturnNoopProviderWhenDisabled() {
        val factory = MetricsProviderFactory()
        val metricsProvider = factory.create()
        assertInstanceOf(NoOpMetricsProvider::class.java, metricsProvider)
    }
}
