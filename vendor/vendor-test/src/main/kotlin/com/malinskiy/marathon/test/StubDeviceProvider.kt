package com.malinskiy.marathon.test

import com.malinskiy.marathon.actor.unboundedChannel
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.vendor.VendorConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class StubDeviceProvider : DeviceProvider {
    lateinit var coroutineScope: CoroutineScope

    private val channel: Channel<DeviceProvider.DeviceEvent> = unboundedChannel()
    var providingLogic: (suspend (Channel<DeviceProvider.DeviceEvent>) -> Unit)? = null

    override val deviceInitializationTimeoutMillis: Long = 180_000
    override suspend fun initialize(vendorConfiguration: VendorConfiguration) {
    }

    override fun subscribe(): Channel<DeviceProvider.DeviceEvent> {
        providingLogic?.let {
            coroutineScope.launch {
                providingLogic?.invoke(channel)
            }
        }

        return channel
    }

    override suspend fun terminate() {
        channel.close()
    }
}
