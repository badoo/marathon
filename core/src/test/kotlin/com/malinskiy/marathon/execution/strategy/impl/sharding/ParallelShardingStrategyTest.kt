package com.malinskiy.marathon.execution.strategy.impl.sharding

import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParallelShardingStrategyTest {
    @Test
    fun `should save list of tests to TestShard without modifications`() {
        val strategy = ParallelShardingStrategy()
        val tests = stubTests(100)
        val shard = strategy.createShard(tests)

        assertThat(shard.tests).containsExactlyElementsOf(tests)
        assertThat(shard.flakyTests).isEmpty()
    }
}
