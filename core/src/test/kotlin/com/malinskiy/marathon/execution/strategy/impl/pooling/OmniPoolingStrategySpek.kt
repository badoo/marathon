package com.malinskiy.marathon.execution.strategy.impl.pooling

import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.DeviceStub
import com.malinskiy.marathon.device.NetworkState
import com.malinskiy.marathon.device.OperatingSystem
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class OmniPoolingStrategySpek : Spek({
    describe("omni strategy") {
        val strategy = OmniPoolingStrategy()
        val device1 = DeviceStub(OperatingSystem("22"), "Serial", NetworkState.CONNECTED, true)
        val device2 = DeviceStub(OperatingSystem("25"), "Serial2", NetworkState.DISCONNECTED, false)

        it("should return same DevicePoolId for all devices") {
            assertEquals(DevicePoolId("omni"), strategy.associate(device1))
            assertEquals(DevicePoolId("omni"), strategy.associate(device2))
        }
    }
})
