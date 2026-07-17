package com.malinskiy.marathon.report.junit

import com.malinskiy.marathon.analytics.internal.sub.Event
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.report.Reporter

internal class JUnitReporter(private val jUnitWriter: JUnitWriter) : Reporter {
    override fun event(event: Event) {
        if (event is TestEvent) {
            jUnitWriter.testFinished(event.poolId, event.device, event.testResult, null)
        }
    }

    override fun generate(executionReport: ExecutionReport) = Unit
}
