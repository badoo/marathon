package com.malinskiy.marathon.android.executor.logcat.model

enum class LogLevel(
    val priority: Int,
    val levelName: String,
    val letter: Char,
) {
    VERBOSE(2, "verbose", 'V'),
    DEBUG(3, "debug", 'D'),
    INFO(4, "info", 'I'),
    WARN(5, "warn", 'W'),
    ERROR(6, "error", 'E'),
    ASSERT(7, "assert", 'A'),
}
