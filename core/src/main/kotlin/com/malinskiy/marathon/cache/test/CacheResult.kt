package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.test.Test

sealed class CacheResult {
    data class Hit(
        val pool: DevicePoolId,
        val testResult: TestResult,
    ) : CacheResult()

    data class Miss(
        val pool: DevicePoolId,
        val test: Test,
    ) : CacheResult()
}
