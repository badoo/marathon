package com.malinskiy.marathon.execution

import mu.KLogger

fun KLogger.measure(name: String, task: () -> Unit) {
    val startTime = System.currentTimeMillis()
    task()
    val executionTime = System.currentTimeMillis() - startTime
    debug("$name took $executionTime ms")
}

suspend fun KLogger.measureAsync(name: String, task: (suspend () -> Unit)) {
    val startTime = System.currentTimeMillis()
    task()
    val executionTime = System.currentTimeMillis() - startTime
    debug("$name took $executionTime ms")
}
