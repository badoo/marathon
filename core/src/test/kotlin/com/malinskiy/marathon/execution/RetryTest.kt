package com.malinskiy.marathon.execution

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RetryTest {
    @Test
    fun `GIVEN block succeeds after a failure WHEN retrying THEN returns normally`() = runTest {
        var attempts = 0

        withRetry(maxAttempts = 3, retryDelay = Duration.ofMillis(100)) {
            attempts++
            if (attempts < 2) {
                error("Attempt $attempts failed")
            }
        }

        assertThat(attempts).isEqualTo(2)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `GIVEN block always fails WHEN retrying THEN rethrows the exception after maxAttempts`() = runTest {
        var attempts = 0

        assertThrows<IllegalStateException> {
            withRetry(maxAttempts = 3, retryDelay = Duration.ofMillis(100)) {
                attempts++
                error("Attempt $attempts failed")
            }
        }

        assertThat(attempts).isEqualTo(3)
        assertThat(currentTime).isEqualTo(200)
    }

    @Test
    fun `GIVEN block throws CancellationException WHEN retrying THEN retries up to maxAttempts`() = runTest {
        var attempts = 0

        assertThrows<CancellationException> {
            withRetry(maxAttempts = 3, retryDelay = Duration.ofMillis(100)) {
                attempts++
                throw CancellationException("Cancellation from a library")
            }
        }

        assertThat(attempts).isEqualTo(3)
    }

    @Test
    fun `GIVEN coroutine cancelled during attempt WHEN block throws CancellationException THEN rethrows the cancellation without retrying`() = runTest {
        var attempts = 0
        var completedNormally = false

        val job = launch {
            withRetry(maxAttempts = 3, retryDelay = Duration.ZERO) {
                attempts++
                cancel()
                throw CancellationException("Cancellation from a library")
            }
            completedNormally = true
        }
        job.join()

        assertThat(attempts).isEqualTo(1)
        assertThat(completedNormally).isFalse()
    }

    @Test
    fun `GIVEN calling coroutine is cancelled WHEN retrying THEN rethrows the cancellation without running the block`() = runTest {
        var attempts = 0
        var completedNormally = false

        val job = launch {
            cancel()
            withRetry(maxAttempts = 3, retryDelay = Duration.ZERO) {
                attempts++
            }
            completedNormally = true
        }
        job.join()

        assertThat(attempts).isEqualTo(0)
        assertThat(completedNormally).isFalse()
    }

    @Test
    fun `GIVEN block times out WHEN retrying THEN retries up to maxAttempts`() = runTest {
        var attempts = 0

        assertThrows<TimeoutCancellationException> {
            withRetry(maxAttempts = 3, retryDelay = Duration.ofMillis(100)) {
                attempts++
                withTimeout(1.milliseconds) {
                    delay(100.milliseconds)
                }
            }
        }

        assertThat(attempts).isEqualTo(3)
    }

    @Test
    fun `GIVEN enclosing timeout expires WHEN retrying THEN rethrows immediately without retrying`() = runTest {
        var attempts = 0

        assertThrows<TimeoutCancellationException> {
            withTimeout(50.milliseconds) {
                withRetry(maxAttempts = 3, retryDelay = Duration.ZERO) {
                    attempts++
                    delay(100.milliseconds)
                }
            }
        }

        assertThat(attempts).isEqualTo(1)
    }

    @Test
    fun `GIVEN block throws InterruptedException WHEN retrying THEN rethrows immediately without setting the interrupt flag`() = runTest {
        var attempts = 0

        assertThrows<InterruptedException> {
            withRetry(maxAttempts = 3, retryDelay = Duration.ofMillis(100)) {
                attempts++
                throw InterruptedException("Interrupted")
            }
        }
        // Read and clear the interrupt flag: a leaked flag would poison subsequent tests on this thread
        val interrupted = Thread.interrupted()

        assertThat(attempts).isEqualTo(1)
        assertThat(interrupted).isFalse()
    }

    @Test
    fun `GIVEN block throws Error WHEN retrying THEN rethrows immediately without retrying`() = runTest {
        var attempts = 0

        assertThrows<OutOfMemoryError> {
            withRetry(maxAttempts = 3, retryDelay = Duration.ofMillis(100)) {
                attempts++
                throw OutOfMemoryError("Simulated")
            }
        }

        assertThat(attempts).isEqualTo(1)
    }
}
