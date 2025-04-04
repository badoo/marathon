package com.malinskiy.marathon.report.junit

import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.report.Reporter
import com.malinskiy.marathon.report.summary.TestSummary
import com.malinskiy.marathon.test.Test

internal class FinalJUnitReporter(private val jUnitWriter: JUnitWriter) : Reporter {
    override fun generate(executionReport: ExecutionReport) {
        val summaries: Map<Test, TestSummary> = executionReport.testSummaries

        executionReport
            .testEvents
            .filter { it.final }
            .forEach { event ->
                val summary = summaries[event.testResult.test].takeIf { it?.results?.any(TestResult::isFailedOrBroken) == true }
                jUnitWriter.testFinished(event.poolId, event.device, event.testResult, summary)
            }
    }
}
