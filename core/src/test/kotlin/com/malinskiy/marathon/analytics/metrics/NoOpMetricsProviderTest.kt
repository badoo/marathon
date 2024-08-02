package com.malinskiy.marathon.analytics.metrics

import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.test.TestComponentInfo
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.Instant
import com.malinskiy.marathon.test.Test as MarathonTest

class NoOpMetricsProviderTest {

    @Test
    fun shouldReturn0AsSuccessRate() {
        val test = MarathonTest("pkg", "clazz", "method", emptyList(), TestComponentInfo())
        NoOpMetricsProvider().successRate(test, Instant.now()) `should be equal to` 0.0
    }

    @Test
    fun shouldReturn0AsExecutionTime() {
        val test = MarathonTest("pkg", "clazz", "method", emptyList(), TestComponentInfo())
        NoOpMetricsProvider().executionTime(test, 90.0, Instant.now()) `should be equal to` 0.0
    }
}
