package com.malinskiy.marathon.execution.strategy.impl.flakiness

import com.malinskiy.marathon.MetricsProviderStub
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.generateTests
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class IgnoreFlakinessStrategySpek : Spek({
    describe("ignore flakiness strategy test") {
        it("should return same test shard") {
            val tests = generateTests(10)
            val shard = TestShard(tests)
            val strategy = IgnoreFlakinessStrategy()
            val metricsProvider = MetricsProviderStub()
            val result = strategy.process(shard, metricsProvider)
            assertEquals(shard, result)
        }
    }
})
