package com.malinskiy.marathon.report

import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import java.util.concurrent.ConcurrentLinkedDeque

class StubReporter : Reporter {
    private val captured = ConcurrentLinkedDeque<ExecutionReport>()
    val reports: List<ExecutionReport> get() = captured.toList()

    override suspend fun generate(executionReport: ExecutionReport) {
        captured.add(executionReport)
    }
}
