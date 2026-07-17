package com.malinskiy.marathon.report

import com.malinskiy.marathon.analytics.internal.sub.Event
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport

interface Reporter {
    fun event(event: Event) = Unit

    fun generate(executionReport: ExecutionReport) = Unit
}
