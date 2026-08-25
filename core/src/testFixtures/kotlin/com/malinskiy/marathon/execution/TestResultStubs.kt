package com.malinskiy.marathon.execution

import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.stubDeviceInfo
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.stubTest

fun stubTestResult(
    test: Test = stubTest(),
    device: DeviceInfo = stubDeviceInfo(),
    status: TestStatus = TestStatus.PASSED,
    startTime: Long = 0,
    endTime: Long = 10000,
    batchId: String = "test_batch_id",
    isFromCache: Boolean = false,
    stacktrace: String? = null,
): TestResult = TestResult(
    test = test,
    device = device,
    status = status,
    startTime = startTime,
    endTime = endTime,
    batchId = batchId,
    isFromCache = isFromCache,
    stacktrace = stacktrace,
)

fun stubTestResults(tests: List<Test>): List<TestResult> = tests.map { stubTestResult(it) }
