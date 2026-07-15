package com.malinskiy.marathon.actor

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.selects.SelectClause2
import kotlin.coroutines.CoroutineContext

abstract class Actor<in T>(
    name: String,
    context: CoroutineContext,
    parent: Job? = null
) : SendChannel<T> {

    protected abstract suspend fun receive(msg: T)

    protected val scope = CoroutineScope(context + Job(parent) + CoroutineName(name))

    @OptIn(ObsoleteCoroutinesApi::class)
    private val delegate = scope.actor<T>(capacity = Channel.UNLIMITED) {
        for (msg in channel) {
            receive(msg)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override val isClosedForSend: Boolean
        get() = delegate.isClosedForSend

    override val onSend: SelectClause2<T, SendChannel<T>>
        get() = delegate.onSend

    override fun invokeOnClose(handler: (cause: Throwable?) -> Unit) {
        delegate.invokeOnClose(handler)
    }

    override fun close(cause: Throwable?): Boolean {
        scope.cancel()
        return true
    }

    @Deprecated(
        message = "Deprecated in the favour of 'trySend' method",
        replaceWith = ReplaceWith("trySend(element).isSuccess"),
        level = DeprecationLevel.ERROR
    )
    override fun offer(element: T): Boolean = delegate.trySend(element).isSuccess

    override fun trySend(element: T): ChannelResult<Unit> = delegate.trySend(element)

    override suspend fun send(element: T) = delegate.send(element)
}
