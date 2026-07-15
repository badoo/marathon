package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.generateTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RandomOrderSortingStrategyTest {
    private val strategy = RandomOrderSortingStrategy()

    @Test
    fun `should return 7 tests randomly sorted at least 1 time out of 7`() {
        val tests = generateTests(7)
        val results = List(7) { tests.sortedWith(strategy.process(MetricsProviderStub())) }

        assertThat(results).anyMatch { it != tests }
    }
}
