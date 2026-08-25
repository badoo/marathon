package com.malinskiy.marathon.exceptions

class ReportGenerationException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
