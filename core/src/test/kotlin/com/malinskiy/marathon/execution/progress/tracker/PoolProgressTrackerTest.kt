package com.malinskiy.marathon.execution.progress.tracker

import com.malinskiy.marathon.test.TestComponentInfo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class PoolProgressTrackerTest {
    val test = MarathonTest(
        pkg = "com.malinskiy.marathon",
        clazz = "SomeTest",
        method = "someMethod",
        metaProperties = emptyList(),
        componentInfo = TestComponentInfo()
    )

    @Test
    fun nonStrictMode_case1() {
        val tracker = PoolProgressTracker(strictMode = false)
        tracker.addTests(1)
        tracker.testStarted(test)
        tracker.testPassed(test)
        tracker.testFailed(test)
        assertTrue(tracker.aggregateResult())
        tracker.testPassed(test)
        assertTrue(tracker.aggregateResult())
    }

    @Test
    fun strictMode_case1() {
        val tracker = PoolProgressTracker(strictMode = true)
        tracker.addTests(1)
        tracker.testStarted(test)
        tracker.testPassed(test)
        tracker.testFailed(test)
        assertFalse(tracker.aggregateResult())
        tracker.testPassed(test)
        assertFalse(tracker.aggregateResult())
    }

    @Test
    fun all_incomplete() {
        val tracker = PoolProgressTracker(strictMode = false).apply {
            addTests(1)
        }
        assertFalse(tracker.aggregateResult())
    }

    @Test
    fun withRetries() {
        val tracker = PoolProgressTracker(strictMode = false)

        tracker.addTests(1)
        tracker.testStarted(test)
        tracker.testFailed(test)
        tracker.addTestRetries(1)
        tracker.testStarted(test)
        tracker.testPassed(test)
        assertTrue(tracker.aggregateResult())
    }

    @Test
    fun withRuntimeDiscovery() {
        val tracker = PoolProgressTracker(strictMode = false)
        val test0 = MarathonTest(
            pkg = "com.malinskiy.marathon",
            clazz = "ParameterizedTest",
            method = "test[0]",
            metaProperties = emptyList(),
            componentInfo = TestComponentInfo()
        )
        val test1 = MarathonTest(
            pkg = "com.malinskiy.marathon",
            clazz = "ParameterizedTest",
            method = "test[1]",
            metaProperties = emptyList(),
            componentInfo = TestComponentInfo()
        )

        tracker.addTests(1)
        tracker.testStarted(test0)
        tracker.testPassed(test0)
        tracker.testStarted(test1)
        tracker.testPassed(test1)
        tracker.addTestDiscoveredDuringRuntime(test1)
        assertTrue(tracker.aggregateResult())
    }
}
