package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.analytics.external.StubMetricsProvider
import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RandomOrderSortingStrategyTest {
    private val strategy = RandomOrderSortingStrategy()

    @Test
    fun `should return 7 tests randomly sorted at least 1 time out of 7`() {
        val tests = stubTests(7)
        val results = List(7) { tests.sortedWith(strategy.process(StubMetricsProvider())) }

        assertThat(results).anyMatch { it != tests }
    }
}
