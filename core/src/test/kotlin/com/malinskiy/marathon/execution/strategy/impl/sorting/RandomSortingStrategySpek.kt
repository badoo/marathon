package com.malinskiy.marathon.execution.strategy.impl.sorting

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.generateTests
import org.amshove.kluent.`should match at least one of`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class RandomSortingStrategySpek : Spek(
    {
        describe("random-sorting-strategy test") {
            context("strategy with random ordering") {
                val strategy = RandomSortingStrategy()
                group("single test shard") {
                    val tests = generateTests(3)
                    val testShard = TestShard(tests)
                    it("should return 3 tests randomly sorted at least 1 time out of 3") {
                        val results = List(3) { testShard.tests.sortedWith(strategy.process(MetricsProviderStub())) }
                        results `should match at least one of` {
                            it[0] != tests[0] || it[1] != tests[1] || it[2] != tests[2]
                        }
                    }
                }
            }
        }
    })
