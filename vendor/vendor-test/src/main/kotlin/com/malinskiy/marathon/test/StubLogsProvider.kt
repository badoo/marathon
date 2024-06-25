package com.malinskiy.marathon.test

import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.LogReport
import com.malinskiy.marathon.report.logs.LogsProvider

class StubLogsProvider : LogsProvider {
    override fun getFullReport(): LogReport = LogReport(emptyMap())
    override suspend fun getBatchReport(batchId: String): BatchLogs? = null
}
