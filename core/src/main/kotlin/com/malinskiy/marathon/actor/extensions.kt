package com.malinskiy.marathon.actor

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

fun <T> unboundedChannel() = Channel<T>(Channel.UNLIMITED)

@OptIn(DelicateCoroutinesApi::class)
suspend fun <T> SendChannel<T>.safeSend(element: T) {
    if (isClosedForSend) return
    send(element)
}

/**
 * Consumes every element of the channel, running [action] for up to [concurrency] elements at a time,
 * and suspends until the channel is closed and all actions have completed.
 */
suspend fun <T> ReceiveChannel<T>.consumeConcurrently(concurrency: Int, action: suspend (T) -> Unit) {
    require(concurrency > 0) { "concurrency must be positive, got $concurrency" }
    coroutineScope {
        val permits = Semaphore(concurrency)
        for (element in this@consumeConcurrently) {
            launch { permits.withPermit { action(element) } }
        }
    }
}
