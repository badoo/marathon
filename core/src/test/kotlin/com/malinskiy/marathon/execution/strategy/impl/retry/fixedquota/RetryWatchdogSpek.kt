package com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class RetryWatchdogSpek : Spek({
    describe("retry watchdog test") {
        group("total allowed quota tests") {
            it("should return false if total allowed quota is 0") {
                val watchdog = RetryWatchdog(0, 3)
                assertFalse(watchdog.requestRetry(1))
            }
            it("should return true if total allowed quota is not 0") {
                val watchdog = RetryWatchdog(100, 3)
                assertTrue(watchdog.requestRetry(1))
            }
        }
        group("max retry per test quota") {
            it("should return false if max retry per test quota is 0 and input is 1") {
                val watchdog = RetryWatchdog(100, 0)
                assertFalse(watchdog.requestRetry(1))
            }
            it("should return false if max retry per test quota is 2 and input is 3") {
                val watchdog = RetryWatchdog(100, 2)
                assertFalse(watchdog.requestRetry(3))
            }
            it("should return true if max retry per test quota is 3 and input is 1") {
                val watchdog = RetryWatchdog(100, 3)
                assertTrue(watchdog.requestRetry(1))
            }
            it("should return true if max retry per test quota is 2 and input is 1") {
                val watchdog = RetryWatchdog(100, 2)
                assertTrue(watchdog.requestRetry(1))
            }
        }
    }
})
