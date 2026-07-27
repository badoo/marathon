package com.malinskiy.marathon.report.logs

class StubLogsProvider : LogsProvider {
    var logs: Map<String, BatchLogs> = emptyMap()

    override suspend fun getFullReport(): LogReport = LogReport(logs)
    override suspend fun getBatchReport(batchId: String): BatchLogs? = logs[batchId]
}
