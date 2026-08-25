package com.malinskiy.marathon.report.summary

import com.malinskiy.marathon.execution.TestResult

data class Batch(
    val batchId: String,
    val testResults: List<TestResult>,
)
