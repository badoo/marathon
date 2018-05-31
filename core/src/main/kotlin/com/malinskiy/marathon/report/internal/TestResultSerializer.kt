package com.malinskiy.marathon.report.internal

import com.google.gson.Gson
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import java.io.FileWriter


class TestResultSerializer(private val fileManager: FileManager,
                           private val gson: Gson) {
    fun testFinished(poolId: DevicePoolId, device: Device, testResult: TestResult) {
        val file = fileManager.createFile(FileType.TEST_RESULT, poolId, device, testResult.test)
        file.writeText(gson.toJson(testResult))
    }
}