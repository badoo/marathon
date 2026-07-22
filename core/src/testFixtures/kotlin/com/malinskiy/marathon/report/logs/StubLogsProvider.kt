package com.malinskiy.marathon.report.logs

class StubLogsProvider(private val logs: Map<String, BatchLogs> = emptyMap()) : LogsProvider {
    override suspend fun getFullReport(): LogReport = LogReport(logs)
    override suspend fun getBatchReport(batchId: String): BatchLogs? = logs[batchId]
}
