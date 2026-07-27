package com.malinskiy.marathon.actor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConsumeConcurrentlyTest {
    @Test
    fun `runs up to the concurrency limit simultaneously`() = runTest {
        val concurrency = 4
        val total = 20
        val inFlight = AtomicInteger()
        val peak = AtomicInteger()
        val channel = unboundedChannel<Int>()
        repeat(total) { channel.trySend(it) }
        channel.close()

        channel.consumeConcurrently(concurrency) {
            peak.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            delay(10.milliseconds)
            inFlight.decrementAndGet()
        }

        assertThat(peak.get()).isEqualTo(concurrency)
    }

    @Test
    fun `processes every element exactly once and waits for all to finish`() = runTest {
        val total = 50
        val processed = ConcurrentLinkedQueue<Int>()
        val channel = unboundedChannel<Int>()
        repeat(total) { channel.trySend(it) }
        channel.close()

        channel.consumeConcurrently(concurrency = 8) {
            delay(1.milliseconds)
            processed.add(it)
        }

        assertThat(processed).containsExactlyInAnyOrderElementsOf(0 until total)
    }

    @Test
    fun `reaches and never exceeds the concurrency limit on a real dispatcher`() = runTest {
        val concurrency = 6
        val total = 100
        val inFlight = AtomicInteger()
        val peak = AtomicInteger()
        val fullBatchRunning = CompletableDeferred<Unit>()
        val processed = ConcurrentLinkedQueue<Int>()
        val channel = unboundedChannel<Int>()
        repeat(total) { channel.trySend(it) }
        channel.close()

        withContext(Dispatchers.Default) {
            channel.consumeConcurrently(concurrency) {
                val running = inFlight.incrementAndGet()
                peak.accumulateAndGet(running, ::maxOf)
                if (running == concurrency) {
                    fullBatchRunning.complete(Unit)
                }
                fullBatchRunning.await()
                inFlight.decrementAndGet()
                processed.add(it)
            }
        }

        assertThat(peak.get()).isEqualTo(concurrency)
        assertThat(processed).containsExactlyInAnyOrderElementsOf(0 until total)
    }

    @Test
    fun `propagates action failure and cancels in-flight siblings`() = runTest {
        val concurrency = 4
        val started = AtomicInteger()
        val completedNormally = AtomicInteger()
        val allStarted = CompletableDeferred<Unit>()
        val channel = unboundedChannel<Int>()
        repeat(concurrency) { channel.trySend(it) }
        channel.close()

        val thrown = assertThrows<IllegalStateException> {
            channel.consumeConcurrently(concurrency) { element ->
                if (started.incrementAndGet() == concurrency) {
                    allStarted.complete(Unit)
                }
                allStarted.await()
                if (element == 0) {
                    error("boom")
                }
                delay(10.seconds)
                completedNormally.incrementAndGet()
            }
        }

        assertThat(thrown).hasMessage("boom")
        assertThat(started.get()).isEqualTo(concurrency)
        assertThat(completedNormally.get()).isZero()
    }

    @Test
    fun `rejects non-positive concurrency`() = runTest {
        val channel = unboundedChannel<Int>()
        channel.close()

        assertThrows<IllegalArgumentException> { channel.consumeConcurrently(0) { } }
    }
}
