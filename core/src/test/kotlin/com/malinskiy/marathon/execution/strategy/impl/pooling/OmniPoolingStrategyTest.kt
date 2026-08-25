package com.malinskiy.marathon.execution.strategy.impl.pooling

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.NetworkState
import com.malinskiy.marathon.device.OperatingSystem
import com.malinskiy.marathon.device.StubDevice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OmniPoolingStrategyTest {
    private val strategy = OmniPoolingStrategy()

    @Test
    fun `should return same DevicePoolId for all devices`() {
        val device1 = StubDevice(
            operatingSystem = OperatingSystem("22"),
            networkState = NetworkState.CONNECTED,
            serialNumber = "Serial",
            healthy = true,
        )
        val device2 = StubDevice(
            operatingSystem = OperatingSystem("25"),
            networkState = NetworkState.DISCONNECTED,
            serialNumber = "Serial2",
            healthy = false,
        )
        val poolId1 = strategy.associate(device1)
        val poolId2 = strategy.associate(device2)

        assertThat(poolId1).isEqualTo(DevicePoolId("omni"))
        assertThat(poolId2).isEqualTo(DevicePoolId("omni"))
    }
}
