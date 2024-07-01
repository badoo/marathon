package com.malinskiy.marathon.analytics.external

internal class MetricsProviderFactory {
    fun create(): MetricsProvider = NoOpMetricsProvider()
}
