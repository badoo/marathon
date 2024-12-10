package com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.generateTestResults
import com.malinskiy.marathon.generateTests
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class FixedQuotaRetryStrategySpek : Spek({
    describe("fixed quota retry strategy tests") {
        group("total quota tests") {
            it("total quota is 1") {
                val strategy = FixedQuotaRetryStrategy(totalAllowedRetryQuota = 1)
                val poolId = DevicePoolId("DevicePoolId-1")
                val tests = generateTests(10)
                val testResults = generateTestResults(tests)
                val results = strategy.process(poolId, testResults, emptyList())
                assertEquals(1, results.size)
            }
            it("total quota more than size of the input list") {
                val strategy = FixedQuotaRetryStrategy(totalAllowedRetryQuota = 10 + 1)
                val poolId = DevicePoolId("DevicePoolId-1")
                val tests = generateTests(10)
                val testResults = generateTestResults(tests)
                val results = strategy.process(poolId, testResults, emptyList())
                assertEquals(10, results.size)
            }
        }
        group("flakiness tests") {
            val strategy by memoized { FixedQuotaRetryStrategy() }
            val poolId = DevicePoolId("DevicePoolId-1")
            val tests = generateTests(50)
            val testResults = generateTestResults(tests)
            it("should return all tests if flakytests size = 0") {
                val results = strategy.process(poolId, testResults, emptyList())
                assertEquals(50, results.size)
            }
            it("should return 0 tests if flakiness strategy added 3 flaky tests per test") {
                val results = strategy.process(
                    poolId,
                    testResults,
                    tests + tests + tests
                )
                assertEquals(0, results.size)
            }
        }
    }
})
