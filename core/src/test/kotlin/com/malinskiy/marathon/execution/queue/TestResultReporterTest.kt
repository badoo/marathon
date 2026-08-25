package com.malinskiy.marathon.execution.queue

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.stubDeviceInfo
import com.malinskiy.marathon.execution.SimpleClassnameFilter
import com.malinskiy.marathon.execution.StrictRunConfiguration
import com.malinskiy.marathon.execution.TestFilter
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions

class TestResultReporterTest {
    private val track = mock<Track>()
    private val defaultConfig = configuration()
    private val strictConfig = defaultConfig.copy(strictMode = true)
    private val test = stubTest()
    private val poolId = DevicePoolId("test")
    private val deviceInfo = stubDeviceInfo()

    @Test
    fun `default config, success - failure - failure should report success`() {
        val reporter = defaultReporter()

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 4, endTime = 5)

        reporter.testFinished(deviceInfo, r1)
        reporter.testFailed(deviceInfo, r2)
        reporter.testFailed(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = true)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = false)
            verifyNoMoreInteractions(track)
        }
    }

    @Test
    fun `default config, failure - failure - success should report success`() {
        val reporter = defaultReporter()

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 4, endTime = 5)

        reporter.testFailed(deviceInfo, r1)
        reporter.testFailed(deviceInfo, r2)
        reporter.testFinished(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = true)
            verifyNoMoreInteractions(track)
        }
    }

    @Test
    fun `strict config, success - failure - failure should report failure`() {
        val reporter = strictReporter()

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 4, endTime = 5)

        reporter.testFinished(deviceInfo, r1)
        reporter.testFailed(deviceInfo, r2)
        reporter.testFailed(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = true)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = false)
            verifyNoMoreInteractions(track)
        }
    }

    @Test
    fun `strict config, failure - success - success should report failure`() {
        val reporter = strictReporter()

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 4, endTime = 5)

        reporter.testFailed(deviceInfo, r1)
        reporter.testFinished(deviceInfo, r2)
        reporter.testFinished(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = true)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = false)
            verifyNoMoreInteractions(track)
        }
    }

    @Test
    fun `strict run filter matching test, success - failure - failure should report failure`() {
        val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral(test.clazz)))

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 4, endTime = 5)

        reporter.testFinished(deviceInfo, r1)
        reporter.testFailed(deviceInfo, r2)
        reporter.testFailed(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = true)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = false)
            verifyNoMoreInteractions(track)
        }
    }

    @Test
    fun `strict run filter matching test, failure - success - success should report failure`() {
        val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral(test.clazz)))

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 4, endTime = 5)

        reporter.testFailed(deviceInfo, r1)
        reporter.testFinished(deviceInfo, r2)
        reporter.testFinished(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = true)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = false)
            verifyNoMoreInteractions(track)
        }
    }

    @Test
    fun `strict run filter not matching test, success - failure - failure should report success`() {
        val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral("$^")))

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 4, endTime = 5)

        reporter.testFinished(deviceInfo, r1)
        reporter.testFailed(deviceInfo, r2)
        reporter.testFailed(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = true)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = false)
            verifyNoMoreInteractions(track)
        }
    }

    @Test
    fun `strict run filter not matching test, failure - success - success should report success`() {
        val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral("$^")))

        val r1 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.FAILURE, startTime = 0, endTime = 1)
        val r2 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 2, endTime = 3)
        val r3 = stubTestResult(test = test, device = deviceInfo, status = TestStatus.PASSED, startTime = 4, endTime = 5)

        reporter.testFailed(deviceInfo, r1)
        reporter.testFinished(deviceInfo, r2)
        reporter.testFinished(deviceInfo, r3)

        inOrder(track) {
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r1, final = false)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r2, final = true)
            verify(track).test(poolId = poolId, device = deviceInfo, testResult = r3, final = false)
            verifyNoMoreInteractions(track)
        }
    }

    private fun defaultReporter() = TestResultReporter(
        poolId,
        defaultConfig,
        track,
    ).apply {
        addShard(TestShard(listOf(test, test, test)))
    }

    private fun strictFilterReporter(filter: TestFilter) = TestResultReporter(
        poolId,
        defaultConfig.copy(strictRunConfiguration = StrictRunConfiguration(filter = listOf(filter), runs = 3)),
        track,
    ).apply {
        addShard(TestShard(listOf(test, test, test)))
    }

    private fun strictReporter() = TestResultReporter(
        poolId,
        strictConfig,
        track,
    ).apply {
        addShard(TestShard(listOf(test, test, test)))
    }
}
