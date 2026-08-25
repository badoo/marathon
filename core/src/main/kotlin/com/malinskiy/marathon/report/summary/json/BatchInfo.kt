package com.malinskiy.marathon.report.summary.json

data class BatchInfo(
    val id: String,
    val component: String,
    val tests: List<TestInfo>,
)
