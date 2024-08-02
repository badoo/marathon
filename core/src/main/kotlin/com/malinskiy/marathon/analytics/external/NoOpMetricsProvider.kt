package com.malinskiy.marathon.analytics.external

import com.malinskiy.marathon.test.Test
import java.time.Instant

internal class NoOpMetricsProvider : MetricsProvider {
    override fun executionTime(test: Test, percentile: Double, limit: Instant): Double = 0.0

    override fun successRate(test: Test, limit: Instant): Double = 0.0

    override fun close() = Unit
}
