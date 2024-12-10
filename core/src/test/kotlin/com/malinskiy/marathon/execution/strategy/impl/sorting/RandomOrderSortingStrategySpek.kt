package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.generateTests
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertTrue

class RandomOrderSortingStrategySpek : Spek({
    describe("random-sorting-strategy test") {
        context("strategy with random ordering") {
            val strategy = RandomOrderSortingStrategy()
            group("single test shard") {
                val tests = generateTests(7)
                it("should return 7 tests randomly sorted at least 1 time out of 7") {
                    val results = List(7) { tests.sortedWith(strategy.process(MetricsProviderStub())) }
                    assertTrue(
                        results.any { it.foldIndexed(false) { index, acc, test -> acc || (test != tests[index]) } }
                    )
                }
            }
        }
    }
})
