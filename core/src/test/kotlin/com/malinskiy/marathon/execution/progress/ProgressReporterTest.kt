package com.malinskiy.marathon.execution.progress

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.test.StubDevice
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.factory.ConfigurationFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class ProgressReporterTest {
    private val reporter = ProgressReporter(ConfigurationFactory().build())
    private val deviceInfo = StubDevice().toDeviceInfo()

    @Test
    fun shouldReportProgressForOnePool() {
        val poolId = DevicePoolId("testpool")

        val test1 = MarathonTest("com.example", "SimpleTest", "method1", emptyList(), TestComponentInfo())
        val test2 = MarathonTest("com.example", "SimpleTest", "method2", emptyList(), TestComponentInfo())
        val test3 = MarathonTest("com.example", "SimpleTest", "method3", emptyList(), TestComponentInfo())

        reporter.addTests(poolId, 3)
        assertEquals(0.0f, reporter.progress())

        /**
         * test 1 passed
         */
        reporter.testStarted(poolId, deviceInfo, test1)
        reporter.testPassed(poolId, deviceInfo, test1)
        assertEquals(1 / 3f, reporter.progress())

        /**
         * test 2 failed
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testFailed(poolId, deviceInfo, test2)
        assertEquals(2 / 3f, reporter.progress())

        /**
         * adding 4 retries for test2 and then test 2 passes once
         */
        reporter.addRetries(poolId, 4)
        assertEquals(2 / 7f, reporter.progress())
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testPassed(poolId, deviceInfo, test2)
        assertEquals(3 / 7f, reporter.progress())

        /**
         * 1 retry of test 2 fails
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testFailed(poolId, deviceInfo, test2)
        assertEquals(4 / 7f, reporter.progress())

        /**
         * 1 retry of test 2 is ignored
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testIgnored(poolId, deviceInfo, test2)
        assertEquals(5 / 7f, reporter.progress())

        /**
         * removing one retry of test 2
         */
        reporter.removeTests(poolId, 1)
        assertEquals(5 / 6f, reporter.progress())

        /**
         * test 3 is ignored (assumption failure or just ignore)
         */
        reporter.testStarted(poolId, deviceInfo, test3)
        reporter.testIgnored(poolId, deviceInfo, test3)
        val progress = reporter.progress()
        assertEquals(6 / 6f, progress)
    }

    @Test
    fun shouldReportProgressForOnePoolWithMultipleShards() {
        val poolId = DevicePoolId("testpool")

        val test1 = MarathonTest("com.example", "SimpleTest", "method1", emptyList(), TestComponentInfo())
        val test2 = MarathonTest("com.example", "SimpleTest", "method2", emptyList(), TestComponentInfo())
        val test3 = MarathonTest("com.example", "SimpleTest", "method3", emptyList(), TestComponentInfo())

        // Add the first shard
        reporter.addTests(poolId, 2)
        assertEquals(.0f, reporter.progress())

        /**
         * test 1 passed
         */
        reporter.testStarted(poolId, deviceInfo, test1)
        reporter.testPassed(poolId, deviceInfo, test1)
        assertEquals(1 / 2f, reporter.progress())

        // Add the second shard
        reporter.addTests(poolId, 1)

        /**
         * test 2 passed
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testPassed(poolId, deviceInfo, test2)
        assertEquals(2 / 3f, reporter.progress())

        /**
         * test 3 passed
         */
        reporter.testStarted(poolId, deviceInfo, test3)
        reporter.testPassed(poolId, deviceInfo, test3)
        assertEquals(3 / 3f, reporter.progress())
    }

    @Test
    fun shouldReportProgressForOnePoolWithRuntimeDiscovery() {
        val poolId = DevicePoolId("testpool")

        val test0 = MarathonTest("com.example", "SimpleTest", "method[0]", emptyList(), TestComponentInfo())
        val test1 = MarathonTest("com.example", "SimpleTest", "method[1]", emptyList(), TestComponentInfo())
        val test2 = MarathonTest("com.example", "SimpleTest", "method[2]", emptyList(), TestComponentInfo())

        reporter.addTests(poolId, 1)
        assertEquals(.0f, reporter.progress())

        /**
         * [0] passed
         */
        reporter.testStarted(poolId, deviceInfo, test0)
        reporter.testPassed(poolId, deviceInfo, test0)
        assertEquals(1 / 1f, reporter.progress())

        /**
         * [1] passed
         */
        reporter.testStarted(poolId, deviceInfo, test1)
        reporter.testPassed(poolId, deviceInfo, test1)
        assertEquals(2 / 1f, reporter.progress())

        /**
         * [2] passed
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testPassed(poolId, deviceInfo, test2)
        assertEquals(3 / 1f, reporter.progress())

        reporter.addTestDiscoveredDuringRuntime(poolId, test1)
        reporter.addTestDiscoveredDuringRuntime(poolId, test2)

        val progress = reporter.progress()
        assertEquals(6 / 6f, progress)
    }
}
