package com.malinskiy.marathon.execution

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.time.delay
import java.time.Duration

suspend fun withRetry(maxAttempts: Int, retryDelay: Duration, block: suspend () -> Unit) {
    check(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    currentCoroutineContext().ensureActive()

    var attempt = 1
    while (true) {
        try {
            return block()
        } catch (e: InterruptedException) {
            throw e
        } catch (e: Exception) {
            // A CancellationException thrown by the block itself (an inner withTimeout expiring, a cancelled future)
            // is a retryable failure, unlike cancellation of this coroutine which ensureActive() rethrows
            currentCoroutineContext().ensureActive()
            if (attempt == maxAttempts) {
                throw e
            }
            delay(retryDelay)
        }
        ++attempt
    }
}
