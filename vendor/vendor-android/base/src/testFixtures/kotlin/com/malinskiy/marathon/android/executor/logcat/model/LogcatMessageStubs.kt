package com.malinskiy.marathon.android.executor.logcat.model

import java.time.Instant

fun stubLogcatMessage(
    timestamp: Instant = Instant.ofEpochMilli(1585850200000L),
    processId: Int = 0,
    threadId: Int = 0,
    applicationName: String = "test",
    logLevel: LogLevel = LogLevel.ERROR,
    tag: String = "test",
    body: String = "test",
): LogcatMessage = LogcatMessage(
    timestamp = timestamp,
    processId = processId,
    threadId = threadId,
    applicationName = applicationName,
    logLevel = logLevel,
    tag = tag,
    body = body,
)
