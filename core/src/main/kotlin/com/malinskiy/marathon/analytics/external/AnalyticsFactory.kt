package com.malinskiy.marathon.analytics.external

class AnalyticsFactory {

    private val metricsFactory = MetricsProviderFactory()

    fun create(): Analytics = Analytics(metricsFactory.create())
}
