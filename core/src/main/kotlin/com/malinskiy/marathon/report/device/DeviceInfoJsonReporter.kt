package com.malinskiy.marathon.report.device

import com.google.gson.Gson
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.Reporter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class DeviceInfoJsonReporter(
    private val fileManager: FileManager,
    private val gson: Gson
) : Reporter {
    override suspend fun generate(executionReport: ExecutionReport) {
        executionReport.deviceConnectedEvents
            .forEach { event ->
                currentCoroutineContext().ensureActive()
                val json = gson.toJson(event.device)
                fileManager.createFile(FileType.DEVICE_INFO, event.poolId, event.device).writeText(json)
            }
    }
}
