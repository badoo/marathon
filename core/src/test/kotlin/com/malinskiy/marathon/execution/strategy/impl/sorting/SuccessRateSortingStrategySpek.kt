package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.generateTests
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant
import java.time.temporal.ChronoUnit

class SuccessRateSortingStrategySpek : Spek({
    describe("success-rate-sorting-strategy test") {
        context("strategy with default ordering") {
            val strategy = SuccessRateSortingStrategy(Instant.now().minus(1, ChronoUnit.DAYS))
            group("single test shard") {
                val tests = generateTests(3)
                val testShard = TestShard(tests)
                it("should return 3 tests sorted by descending success rate") {
                    val metricsProvider = MetricsProviderStub(
                        successRates = testShard.tests.mapIndexed { index, test ->
                            Pair(test, 0.1 * index)
                        }.toMap()
                    )
                    val result = testShard.tests.sortedWith(strategy.process(metricsProvider))
                    assertEquals(3, result.size)
                    assertEquals(tests[2], result[0])
                    assertEquals(tests[1], result[1])
                    assertEquals(tests[0], result[2])
                }
            }
        }

        context("strategy with ascending ordering") {
            val strategy = SuccessRateSortingStrategy(
                Instant.now().minus(1, ChronoUnit.DAYS),
                ascending = true
            )
            group("single test shard") {
                val tests = generateTests(3)
                val testShard = TestShard(tests)
                it("should return 3 tests sorted by descending success rate") {
                    val metricsProvider = MetricsProviderStub(
                        successRates = testShard.tests.mapIndexed { index, test ->
                            Pair(test, 0.1 * index)
                        }.toMap()
                    )
                    val result = testShard.tests.sortedWith(strategy.process(metricsProvider))
                    assertEquals(3, result.size)
                    assertEquals(tests[0], result[0])
                    assertEquals(tests[1], result[1])
                    assertEquals(tests[2], result[2])
                }
            }
        }

        context("strategy with descending ordering") {
            val strategy = SuccessRateSortingStrategy(
                Instant.now().minus(1, ChronoUnit.DAYS),
                ascending = false
            )
            group("single test shard") {
                val tests = generateTests(3)
                val testShard = TestShard(tests)
                it("should return 3 tests sorted by descending success rate") {
                    val metricsProvider = MetricsProviderStub(
                        successRates = testShard.tests.mapIndexed { index, test ->
                            Pair(test, 0.1 * index)
                        }.toMap()
                    )
                    val result = testShard.tests.sortedWith(strategy.process(metricsProvider))
                    assertEquals(3, result.size)
                    assertEquals(tests[2], result[0])
                    assertEquals(tests[1], result[1])
                    assertEquals(tests[0], result[2])
                }
            }
        }
    }
})
