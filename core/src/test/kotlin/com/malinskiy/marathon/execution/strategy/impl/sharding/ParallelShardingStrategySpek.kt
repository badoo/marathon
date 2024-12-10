package com.malinskiy.marathon.execution.strategy.impl.sharding

import com.malinskiy.marathon.generateTests
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class ParallelShardingStrategySpek : Spek({
    describe("parallel sharding strategy tests") {
        it("should save list of tests to TestShard w/o modifications") {
            val strategy = ParallelShardingStrategy()
            val tests = generateTests(100)
            val shard = strategy.createShard(tests)
            assertEquals(tests.size, shard.tests.size)
            assertEquals(tests, shard.tests)
            assertEquals(0, shard.flakyTests.size)
        }
    }
})
