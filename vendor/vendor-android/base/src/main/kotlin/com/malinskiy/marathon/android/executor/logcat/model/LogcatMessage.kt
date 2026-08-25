package com.malinskiy.marathon.android.executor.logcat.model

import java.time.Instant

class LogcatMessage(
    val timestamp: Instant,
    val processId: Int,
    val threadId: Int,
    val applicationName: String,
    val logLevel: LogLevel,
    val tag: String,
    val body: String,
)
