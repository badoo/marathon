package com.malinskiy.marathon.report.junit

import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.report.Reporter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class JUnitReporter(private val jUnitWriter: JUnitWriter) : Reporter {
    override suspend fun generate(executionReport: ExecutionReport) {
        executionReport.testEvents.forEach { event ->
            currentCoroutineContext().ensureActive()
            jUnitWriter.testFinished(event.poolId, event.device, event.testResult, null)
        }
    }
}
