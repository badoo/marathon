package com.malinskiy.marathon.ios.logparser.listener

import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.toSafeTestName
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CompletableDeferred

class ProgressReportingListener(
    private val device: Device,
    private val poolId: DevicePoolId,
    private val testBatch: TestBatch,
    private val deferredResults: CompletableDeferred<TestBatchResults>,
    private val progressReporter: ProgressReporter,
    private val testLogListener: TestLogListener,
    private val strictRunChecker: StrictRunChecker,
    private val timer: Timer
) : TestRunListener {

    private val success: MutableList<TestResult> = mutableListOf()
    private val failure: MutableList<TestResult> = mutableListOf()

    override fun batchFinished() {
        val received = (success + failure)
        val receivedTestNames = received.map { it.test.toSafeTestName() }.toHashSet()

        val uncompleted = testBatch.tests.filter {
            !receivedTestNames.contains(it.toSafeTestName())
        }.createUncompletedTestResults(received)

        deferredResults.complete(TestBatchResults(device, testBatch.componentInfo, success, failure, uncompleted))
    }

    private fun List<Test>.createUncompletedTestResults(received: Collection<TestResult>): Collection<TestResult> {
        val lastCompletedTestEndTime = received.maxBy { it.endTime }?.endTime ?: timer.currentTimeMillis()
        return map {
            TestResult(
                test = it,
                device = device.toDeviceInfo(),
                status = TestStatus.FAILURE,
                startTime = lastCompletedTestEndTime,
                endTime = lastCompletedTestEndTime,
                batchId = testBatch.id,
                isStrictRun = strictRunChecker.isStrictRun(it),
                isFromCache = false,
                stacktrace = testLogListener.getLastLog()
            )
        }
    }

    override fun testFailed(test: Test, startTime: Long, endTime: Long) {
        progressReporter.testFailed(poolId, device.toDeviceInfo(), test)
        failure.add(
            TestResult(
                test,
                device.toDeviceInfo(),
                TestStatus.FAILURE,
                startTime,
                endTime,
                testBatch.id,
                strictRunChecker.isStrictRun(test),
                false,
                testLogListener.getLastLog()
            )
        )
    }

    override fun testPassed(test: Test, startTime: Long, endTime: Long) {
        progressReporter.testPassed(poolId, device.toDeviceInfo(), test)
        success.add(
            TestResult(
                test,
                device.toDeviceInfo(),
                TestStatus.PASSED,
                startTime,
                endTime,
                testBatch.id,
                strictRunChecker.isStrictRun(test),
                false,
                testLogListener.getLastLog()
            )
        )
    }

    override fun testStarted(test: Test) {
        progressReporter.testStarted(poolId, device.toDeviceInfo(), test)
    }
}
