package com.malinskiy.marathon.report.test

import com.google.gson.Gson
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.Reporter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class TestJsonReporter(
    private val fileManager: FileManager,
    private val gson: Gson
) : Reporter {
    override suspend fun generate(executionReport: ExecutionReport) {
        for (testEvent in executionReport.testEvents.filter { it.final }) {
            currentCoroutineContext().ensureActive()
            val file = fileManager.createFile(FileType.TEST_RESULT, testEvent.poolId, testEvent.device, testEvent.testResult.test)
            file.writeText(gson.toJson(testEvent.testResult))
        }
    }
}
