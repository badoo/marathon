package com.malinskiy.marathon.device

sealed class DeviceEvent {
    abstract val device: Device

    class DeviceConnected(override val device: Device) : DeviceEvent()
    class DeviceDisconnected(override val device: Device) : DeviceEvent()
}
