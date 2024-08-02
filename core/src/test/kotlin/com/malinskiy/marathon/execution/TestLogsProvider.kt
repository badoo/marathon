package com.malinskiy.marathon.execution

import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.LogReport
import com.malinskiy.marathon.report.logs.LogsProvider

class TestLogsProvider(private val logs: Map<String, BatchLogs>) : LogsProvider {

    override fun getFullReport(): LogReport =
        LogReport(logs)

    override suspend fun getBatchReport(batchId: String): BatchLogs? =
        logs[batchId]
}
