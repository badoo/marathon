package com.malinskiy.marathon.execution.progress.tracker

import com.malinskiy.marathon.test.TestComponentInfo
import org.amshove.kluent.shouldBeEqualTo
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
        tracker.aggregateResult().shouldBeEqualTo(true)
        tracker.testPassed(test)
        tracker.aggregateResult().shouldBeEqualTo(true)
    }

    @Test
    fun strictMode_case1() {
        val tracker = PoolProgressTracker(strictMode = true)
        tracker.addTests(1)
        tracker.testStarted(test)
        tracker.testPassed(test)
        tracker.testFailed(test)
        tracker.aggregateResult().shouldBeEqualTo(false)
        tracker.testPassed(test)
        tracker.aggregateResult().shouldBeEqualTo(false)
    }

    @Test
    fun all_incomplete() {
        val tracker = PoolProgressTracker(strictMode = false).apply {
            addTests(1)
        }
        tracker.aggregateResult() shouldBeEqualTo false
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
        tracker.aggregateResult().shouldBeEqualTo(true)
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
        tracker.aggregateResult().shouldBeEqualTo(true)
    }
}
