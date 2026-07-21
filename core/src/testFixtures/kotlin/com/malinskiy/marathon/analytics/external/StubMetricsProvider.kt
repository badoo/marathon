package com.malinskiy.marathon.analytics.external

import com.malinskiy.marathon.test.Test
import java.time.Instant

class StubMetricsProvider(
    val successRates: Map<Test, Double> = emptyMap(),
    val executionTimes: Map<Test, Double> = emptyMap(),
    val successRate: Double = 0.5,
    val executionTime: Double = 1500.0
) : MetricsProvider {

    override fun successRate(test: Test, limit: Instant): Double =
        successRates[test] ?: successRate

    override fun executionTime(test: Test, percentile: Double, limit: Instant): Double =
        executionTimes[test] ?: executionTime

    override fun close() = Unit
}
