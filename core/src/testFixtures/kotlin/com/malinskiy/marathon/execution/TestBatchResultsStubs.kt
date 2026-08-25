package com.malinskiy.marathon.execution

import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.StubDevice

fun stubTestBatchResults(
    batchId: String = "test_batch_id",
    device: Device = StubDevice(),
    componentInfo: ComponentInfo = StubComponentInfo(),
    finished: List<TestResult> = emptyList(),
    failed: List<TestResult> = emptyList(),
    uncompleted: List<TestResult> = emptyList(),
): TestBatchResults = TestBatchResults(
    batchId = batchId,
    device = device,
    componentInfo = componentInfo,
    finished = finished,
    failed = failed,
    uncompleted = uncompleted,
)
