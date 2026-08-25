package com.malinskiy.marathon.report.listener

import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.execution.MarathonListener
import com.malinskiy.marathon.report.Reporter

class ListenerReporter(private val listener: MarathonListener) : Reporter {
    override suspend fun generate(executionReport: ExecutionReport) {
        listener.onFinished(executionReport)
    }
}
