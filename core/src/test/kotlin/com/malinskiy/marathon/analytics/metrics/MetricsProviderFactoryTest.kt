package com.malinskiy.marathon.analytics.metrics

import com.malinskiy.marathon.analytics.external.MetricsProviderFactory
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class MetricsProviderFactoryTest {

    @Test
    fun shouldReturnNoopProviderWhenDisabled() {
        val factory = MetricsProviderFactory()
        val metricsProvider = factory.create()
        metricsProvider shouldBeInstanceOf NoOpMetricsProvider::class
    }
}
