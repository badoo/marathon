package com.malinskiy.marathon.execution

import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.test.Test

data class TestResult(
    val test: Test,
    val device: DeviceInfo,
    val status: TestStatus,
    val startTime: Long,
    val endTime: Long,
    val batchId: String,
    val isStrictRun: Boolean = false,
    val isFromCache: Boolean = false,
    val stacktrace: String? = null,
    val attachments: List<Attachment> = emptyList()
) {
    fun durationMillis(): Long = endTime - startTime

    val isIgnored: Boolean
        get() = when (status) {
            TestStatus.IGNORED, TestStatus.ASSUMPTION_FAILURE -> true
            else -> false
        }

    val isSuccess: Boolean
        get() = when (status) {
            TestStatus.PASSED -> true
            else -> false
        }

    val isTimeInfoAvailable = startTime != 0L && endTime != 0L

    override fun toString(): String =
        "TestResult(test=${test}, " +
            "device=${device}, status=${status}, " +
            "startTime=${startTime}, endTime=${endTime}, " +
            "isStrictRun=${isStrictRun}," +
            "isFromCache=${isFromCache}, " +
            "stacktrace=${stacktrace?.take(24)})"
}
