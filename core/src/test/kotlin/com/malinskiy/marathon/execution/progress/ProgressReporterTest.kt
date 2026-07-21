package com.malinskiy.marathon.execution.progress

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.test.factory.ConfigurationFactory
import com.malinskiy.marathon.test.stubTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProgressReporterTest {
    private val reporter = ProgressReporter(ConfigurationFactory().build())
    private val deviceInfo = StubDevice().toDeviceInfo()

    @Test
    fun shouldReportProgressForOnePool() {
        val poolId = DevicePoolId("testpool")

        val test1 = stubTest(method = "method1")
        val test2 = stubTest(method = "method2")
        val test3 = stubTest(method = "method3")

        reporter.addTests(poolId, 3)
        assertThat(reporter.progress()).isEqualTo(0.0f)

        /**
         * test 1 passed
         */
        reporter.testStarted(poolId, deviceInfo, test1)
        reporter.testPassed(poolId, deviceInfo, test1)
        assertThat(reporter.progress()).isEqualTo(1 / 3f)

        /**
         * test 2 failed
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testFailed(poolId, deviceInfo, test2)
        assertThat(reporter.progress()).isEqualTo(2 / 3f)

        /**
         * adding 4 retries for test2 and then test 2 passes once
         */
        reporter.addRetries(poolId, 4)
        assertThat(reporter.progress()).isEqualTo(2 / 7f)
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testPassed(poolId, deviceInfo, test2)
        assertThat(reporter.progress()).isEqualTo(3 / 7f)

        /**
         * 1 retry of test 2 fails
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testFailed(poolId, deviceInfo, test2)
        assertThat(reporter.progress()).isEqualTo(4 / 7f)

        /**
         * 1 retry of test 2 is ignored
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testIgnored(poolId, deviceInfo, test2)
        assertThat(reporter.progress()).isEqualTo(5 / 7f)

        /**
         * removing one retry of test 2
         */
        reporter.removeTests(poolId, 1)
        assertThat(reporter.progress()).isEqualTo(5 / 6f)

        /**
         * test 3 is ignored (assumption failure or just ignore)
         */
        reporter.testStarted(poolId, deviceInfo, test3)
        reporter.testIgnored(poolId, deviceInfo, test3)
        val progress = reporter.progress()
        assertThat(progress).isEqualTo(6 / 6f)
    }

    @Test
    fun shouldReportProgressForOnePoolWithMultipleShards() {
        val poolId = DevicePoolId("testpool")

        val test1 = stubTest(method = "method1")
        val test2 = stubTest(method = "method2")
        val test3 = stubTest(method = "method3")

        // Add the first shard
        reporter.addTests(poolId, 2)
        assertThat(reporter.progress()).isEqualTo(.0f)

        /**
         * test 1 passed
         */
        reporter.testStarted(poolId, deviceInfo, test1)
        reporter.testPassed(poolId, deviceInfo, test1)
        assertThat(reporter.progress()).isEqualTo(1 / 2f)

        // Add the second shard
        reporter.addTests(poolId, 1)

        /**
         * test 2 passed
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testPassed(poolId, deviceInfo, test2)
        assertThat(reporter.progress()).isEqualTo(2 / 3f)

        /**
         * test 3 passed
         */
        reporter.testStarted(poolId, deviceInfo, test3)
        reporter.testPassed(poolId, deviceInfo, test3)
        assertThat(reporter.progress()).isEqualTo(3 / 3f)
    }

    @Test
    fun shouldReportProgressForOnePoolWithRuntimeDiscovery() {
        val poolId = DevicePoolId("testpool")

        val test0 = stubTest(method = "method[0]")
        val test1 = stubTest(method = "method[1]")
        val test2 = stubTest(method = "method[2]")

        reporter.addTests(poolId, 1)
        assertThat(reporter.progress()).isEqualTo(.0f)

        /**
         * [0] passed
         */
        reporter.testStarted(poolId, deviceInfo, test0)
        reporter.testPassed(poolId, deviceInfo, test0)
        assertThat(reporter.progress()).isEqualTo(1 / 1f)

        /**
         * [1] passed
         */
        reporter.testStarted(poolId, deviceInfo, test1)
        reporter.testPassed(poolId, deviceInfo, test1)
        assertThat(reporter.progress()).isEqualTo(2 / 1f)

        /**
         * [2] passed
         */
        reporter.testStarted(poolId, deviceInfo, test2)
        reporter.testPassed(poolId, deviceInfo, test2)
        assertThat(reporter.progress()).isEqualTo(3 / 1f)

        reporter.addTestDiscoveredDuringRuntime(poolId, test1)
        reporter.addTestDiscoveredDuringRuntime(poolId, test2)

        val progress = reporter.progress()
        assertThat(progress).isEqualTo(6 / 6f)
    }
}
