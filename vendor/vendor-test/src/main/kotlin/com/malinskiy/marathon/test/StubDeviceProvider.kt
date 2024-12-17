package com.malinskiy.marathon.test

import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.device.DeviceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class StubDeviceProvider : DeviceProvider {
    lateinit var coroutineScope: CoroutineScope

    private val channel: Channel<DeviceEvent> = unboundedChannel()
    var providingLogic: (suspend (Channel<DeviceEvent>) -> Unit)? = null

    override val deviceInitializationTimeoutMillis: Long = 180_000
    override suspend fun initialize() = Unit

    override val deviceEvents: Flow<DeviceEvent>
        get() {
            providingLogic?.let {
                coroutineScope.launch {
                    providingLogic?.invoke(channel)
                }
            }

            return channel.consumeAsFlow()
        }

    override suspend fun terminate() {
        channel.close()
    }

    override fun close() = Unit
}
