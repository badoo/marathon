package com.malinskiy.marathon.report.logs

import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.analytics.internal.sub.TestEventInflator
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.logs.LogEvent.Crash

class LogReportTestEventInflator(private val logsProvider: LogsProvider) : TestEventInflator {
    @Volatile
    private var cachedLogReport: LogReport? = null

    override suspend fun inflate(event: TestEvent): TestEvent {
        val logReport = getLogReport()

        if (!event.testResult.isFailedOrBroken) {
            return event
        }

        val log = getLog(event.testResult, logReport)
        val additionalAttachments = listOfNotNull(
            log?.let { Attachment(log.file, AttachmentType.LOG, FileType.LOG) },
        )

        val newStackTrace = updateStacktrace(event.testResult.stacktrace, log)

        val testResult = event.testResult.copy(
            attachments = event.testResult.attachments + additionalAttachments,
            stacktrace = newStackTrace,
        )

        return event.copy(testResult = testResult)
    }

    private suspend fun getLogReport(): LogReport = cachedLogReport ?: logsProvider.getFullReport().also { cachedLogReport = it }

    private fun updateStacktrace(original: String?, log: Log?): String? {
        if (log == null || log.events.isEmpty()) return original

        val crashEventsDescription = log
            .events
            .filterIsInstance<Crash>()
            .joinToString("\n") {
                "* " + it.message
            }

        return "Crash events:\n${crashEventsDescription}\n\n" + original.orEmpty()
    }

    private fun getLog(testResult: TestResult, logReport: LogReport): Log? {
        val batchId = testResult.batchId
        val logTest = testResult.test.toLogTest()
        val batchLogs = logReport.batches[batchId] ?: return null
        return batchLogs.tests.getOrDefault(logTest, batchLogs.log)
    }
}
