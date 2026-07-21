package com.malinskiy.marathon.execution.strategy.impl.sharding

import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CountShardingStrategyTest {
    @Test
    fun `should return 5 tests for list of 1 test and n = 5`() {
        val strategy = CountShardingStrategy(5)
        val tests = stubTests(1)
        val result = strategy.createShard(tests)

        assertThat(result.tests).hasSize(5)
        assertThat(result.flakyTests).isEmpty()
    }
}
