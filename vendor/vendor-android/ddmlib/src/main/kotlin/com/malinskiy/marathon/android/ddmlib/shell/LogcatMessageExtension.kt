package com.malinskiy.marathon.android.ddmlib.shell

import com.android.ddmlib.Log
import com.malinskiy.marathon.android.executor.logcat.model.LogLevel
import com.malinskiy.marathon.android.executor.logcat.model.LogcatMessage
import com.android.ddmlib.logcat.LogCatMessage as DdmLibLogcatMessage

fun DdmLibLogcatMessage.toMarathonLogcatMessage(): LogcatMessage = LogcatMessage(
    timestamp = header.timestamp,
    processId = header.pid,
    threadId = header.tid,
    applicationName = header.appName,
    logLevel = header.logLevel.asMarathonLogLevel(),
    tag = header.tag,
    body = message,
)

private fun Log.LogLevel.asMarathonLogLevel(): LogLevel = when (this) {
    Log.LogLevel.VERBOSE -> LogLevel.VERBOSE
    Log.LogLevel.DEBUG -> LogLevel.DEBUG
    Log.LogLevel.INFO -> LogLevel.INFO
    Log.LogLevel.WARN -> LogLevel.WARN
    Log.LogLevel.ERROR -> LogLevel.ERROR
    Log.LogLevel.ASSERT -> LogLevel.ASSERT
}
