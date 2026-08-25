package com.malinskiy.marathon.execution

import com.malinskiy.marathon.device.Device

data class TestBatchResults(
    val batchId: String,
    val device: Device,
    val componentInfo: ComponentInfo,
    val finished: Collection<TestResult>,
    val failed: Collection<TestResult>,
    val uncompleted: Collection<TestResult>,
)
