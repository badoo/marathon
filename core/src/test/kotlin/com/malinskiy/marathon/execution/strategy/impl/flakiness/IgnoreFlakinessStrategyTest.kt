package com.malinskiy.marathon.execution.strategy.impl.flakiness

import com.malinskiy.marathon.analytics.external.StubMetricsProvider
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IgnoreFlakinessStrategyTest {
    @Test
    fun `should return same test shard`() {
        val tests = stubTests(10)
        val shard = TestShard(tests)
        val strategy = IgnoreFlakinessStrategy()
        val metricsProvider = StubMetricsProvider()
        val result = strategy.process(shard, metricsProvider)

        assertThat(result).isEqualTo(shard)
    }
}
