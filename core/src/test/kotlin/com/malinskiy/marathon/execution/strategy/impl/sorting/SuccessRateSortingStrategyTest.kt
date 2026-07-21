package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.analytics.external.StubMetricsProvider
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.test.stubTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import java.time.temporal.ChronoUnit

class SuccessRateSortingStrategyTest {
    private val tests = stubTests(3)
    private val testShard = TestShard(tests)
    private val metricsProvider = StubMetricsProvider(
        successRates = testShard.tests.mapIndexed { index, test ->
            Pair(test, 0.1 * index)
        }.toMap()
    )

    @Test
    fun `strategy with default ordering should return 3 tests sorted by descending success rate`() {
        val strategy = SuccessRateSortingStrategy(Instant.now().minus(1, ChronoUnit.DAYS))
        val result = testShard.tests.sortedWith(strategy.process(metricsProvider))

        assertThat(result).containsExactly(tests[2], tests[1], tests[0])
    }

    @ParameterizedTest(name = "strategy with ascending = {0} should return 3 tests sorted by success rate")
    @CsvSource(
        "true, 0, 1, 2",
        "false, 2, 1, 0"
    )
    fun `strategy with explicit ordering should return 3 tests sorted by success rate`(
        ascending: Boolean,
        first: Int,
        second: Int,
        third: Int
    ) {
        val strategy = SuccessRateSortingStrategy(
            Instant.now().minus(1, ChronoUnit.DAYS),
            ascending = ascending
        )
        val result = testShard.tests.sortedWith(strategy.process(metricsProvider))

        assertThat(result).containsExactly(tests[first], tests[second], tests[third])
    }
}
