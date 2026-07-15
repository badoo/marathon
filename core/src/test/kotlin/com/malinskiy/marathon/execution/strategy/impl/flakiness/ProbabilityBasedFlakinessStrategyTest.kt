package com.malinskiy.marathon.execution.strategy.impl.flakiness

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.generateTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

class ProbabilityBasedFlakinessStrategyTest {
    private val strategy = ProbabilityBasedFlakinessStrategy(
        minSuccessRate = 0.8,
        maxCount = 5,
        timeLimit = Instant.now()
    )

    @ParameterizedTest(name = "should return {2} flaky tests for {1} test(s) with success rate = {0}")
    @CsvSource(
        "0.5, 1, 2",
        "0.8, 1, 0",
        "1.0, 1, 0",
        "0.7, 1, 1",
        "0.001, 1, 5",
        "0.7, 3, 3"
    )
    fun `should return flaky tests according to success rate`(successRate: Double, testCount: Int, expectedFlakyTests: Int) {
        val metricsProvider = MetricsProviderStub(successRate = successRate)
        val result = strategy.process(TestShard(generateTests(testCount)), metricsProvider)

        assertThat(result.tests).hasSize(testCount)
        assertThat(result.flakyTests).hasSize(expectedFlakyTests)
    }
}
