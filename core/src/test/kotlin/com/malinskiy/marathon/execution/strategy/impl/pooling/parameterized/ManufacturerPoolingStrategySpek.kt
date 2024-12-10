package com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized

import com.malinskiy.marathon.device.DeviceStub
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class ManufacturerPoolingStrategySpek : Spek({
    describe("pooling strategy based on device manufacturer tests") {
        val strategy by memoized { ManufacturerPoolingStrategy() }
        it("should return DevicePoolId with name equals to device manufacturer") {
            val deviceManufacturer = "TestDeviceManufacturer"
            val device = DeviceStub(manufacturer = deviceManufacturer)
            assertEquals(deviceManufacturer, strategy.associate(device).name)
        }
    }
})
