package com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized

import com.malinskiy.marathon.device.StubDevice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ManufacturerPoolingStrategyTest {
    private val strategy = ManufacturerPoolingStrategy()

    @Test
    fun `should return DevicePoolId with name equals to device manufacturer`() {
        val deviceManufacturer = "TestDeviceManufacturer"
        val device = StubDevice(manufacturer = deviceManufacturer)
        val poolId = strategy.associate(device)

        assertThat(poolId.name).isEqualTo(deviceManufacturer)
    }
}
