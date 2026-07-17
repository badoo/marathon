package com.malinskiy.marathon.report.logs

import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.analytics.internal.sub.TestEventInflator
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.logs.LogEvent.Crash

class LogReportTestEventInflator(private val logsProvider: LogsProvider) : TestEventInflator {
    private val logReport by lazy { logsProvider.getFullReport() }

    override fun inflate(event: TestEvent): TestEvent {

        if (!event.testResult.isFailedOrBroken) {
            return event
        }

        val log = getLog(event.testResult)
        val additionalAttachments = listOfNotNull(
            log?.let { Attachment(log.file, AttachmentType.LOG, FileType.LOG) }
        )

        val newStackTrace = updateStacktrace(event.testResult.stacktrace, log)

        val testResult = event.testResult.copy(
            attachments = event.testResult.attachments + additionalAttachments,
            stacktrace = newStackTrace
        )

        return event.copy(testResult = testResult)
    }

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

    private fun getLog(testResult: TestResult): Log? {
        val batchId = testResult.batchId
        val logTest = testResult.test.toLogTest()
        val batchLogs = logReport.batches[batchId] ?: return null
        return batchLogs.tests.getOrDefault(logTest, batchLogs.log)
    }
}
