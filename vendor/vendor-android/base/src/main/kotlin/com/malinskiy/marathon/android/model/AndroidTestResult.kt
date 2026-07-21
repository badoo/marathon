package com.malinskiy.marathon.android.model

data class AndroidTestResult(
    val status: AndroidTestStatus = AndroidTestStatus.INCOMPLETE,
    val startTime: Long,
    val endTime: Long = 0,
    val stackTrace: String? = null,
    val metrics: Map<String, String> = emptyMap()
)
