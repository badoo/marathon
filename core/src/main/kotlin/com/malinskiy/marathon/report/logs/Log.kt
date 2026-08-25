package com.malinskiy.marathon.report.logs

import java.io.File

class Log(
    val file: File,
    val events: List<LogEvent>,
)
