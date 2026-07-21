package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.analytics.external.StubMetricsProvider
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ExecutionTimeSortingStrategyTest {
    private val strategy = ExecutionTimeSortingStrategy(0.8, Instant.now().minus(1, ChronoUnit.DAYS))

    @Test
    fun `should return 3 tests sorted by execution time`() {
        val tests = stubTests(3)
        val testShard = TestShard(tests)
        val metricsProvider = StubMetricsProvider(
            executionTimes = testShard.tests.mapIndexed { index, test ->
                Pair(test, 1000.0 + index * 1000.0)
            }.toMap()
        )
        val result = testShard.tests.sortedWith(strategy.process(metricsProvider))

        assertThat(result).containsExactly(tests[2], tests[1], tests[0])
    }
}
