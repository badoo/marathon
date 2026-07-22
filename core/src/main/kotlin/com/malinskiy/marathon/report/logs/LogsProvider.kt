package com.malinskiy.marathon.report.logs

interface LogsProvider {
    suspend fun getFullReport(): LogReport
    suspend fun getBatchReport(batchId: String): BatchLogs?
}
