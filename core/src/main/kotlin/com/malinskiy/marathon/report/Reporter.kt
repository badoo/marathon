package com.malinskiy.marathon.report

import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport

interface Reporter {
    suspend fun generate(executionReport: ExecutionReport)
}
