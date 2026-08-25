package com.malinskiy.marathon.report.summary

import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.Test

data class TestSummary(
    val test: Test,
    val results: List<TestResult>,
    val batches: List<Batch>,
) {
    val isFlaky: Boolean by lazy {
        val hasSuccessResult = results.any { it.status == TestStatus.PASSED }
        val hasFailedResult = results.any {
            // stack trace will be null in case of device infra issues, e.g. device offline etc.
            // we will still report these tests as incomplete for stats, but we shouldn't mark them as flaky
            it.status == TestStatus.FAILURE || (it.status == TestStatus.INCOMPLETE && it.stacktrace != null)
        }
        hasSuccessResult && hasFailedResult
    }
}
