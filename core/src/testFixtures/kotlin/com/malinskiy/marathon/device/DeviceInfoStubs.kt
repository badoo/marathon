package com.malinskiy.marathon.device

fun stubDeviceInfo(
    operatingSystem: OperatingSystem = OperatingSystem("Fake OS"),
    serialNumber: String = "fake serial",
    model: String = "fake model",
    manufacturer: String = "fake manufacturer",
    networkState: NetworkState = NetworkState.CONNECTED,
    deviceFeatures: Collection<DeviceFeature> = emptyList(),
    healthy: Boolean = true
): DeviceInfo = DeviceInfo(operatingSystem, serialNumber, model, manufacturer, networkState, deviceFeatures, healthy)
