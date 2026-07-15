package com.malinskiy.marathon.execution.strategy.impl.flakiness

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.generateTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IgnoreFlakinessStrategyTest {
    @Test
    fun `should return same test shard`() {
        val tests = generateTests(10)
        val shard = TestShard(tests)
        val strategy = IgnoreFlakinessStrategy()
        val metricsProvider = MetricsProviderStub()
        val result = strategy.process(shard, metricsProvider)

        assertThat(result).isEqualTo(shard)
    }
}
