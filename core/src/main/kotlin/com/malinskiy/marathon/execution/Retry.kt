package com.malinskiy.marathon.execution

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.delay
import java.time.Duration

suspend fun withRetry(maxAttempts: Int, retryDelay: Duration, block: suspend () -> Unit) {
    check(maxAttempts >= 1) { "maxAttempts must be >= 1" }

    var attempt = 1
    while (currentCoroutineContext().isActive) {
        try {
            return block()
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            currentCoroutineContext().ensureActive()
            if (attempt == maxAttempts) {
                throw e
            } else {
                delay(retryDelay)
            }
        }
        ++attempt
    }
}
