package com.malinskiy.marathon.device

import com.malinskiy.marathon.actor.unboundedChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class StubDeviceProvider : DeviceProvider {
    lateinit var coroutineScope: CoroutineScope

    private val channel: Channel<DeviceEvent> = unboundedChannel()
    var providingLogic: (suspend (Channel<DeviceEvent>) -> Unit)? = null

    override val deviceEvents: Flow<DeviceEvent>
        get() {
            providingLogic?.let {
                coroutineScope.launch {
                    providingLogic?.invoke(channel)
                }
            }

            return channel.consumeAsFlow()
        }

    override suspend fun initialize() = Unit

    override suspend fun terminate() {
        channel.close()
    }

    override fun close() = Unit
}
