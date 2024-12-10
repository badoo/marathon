package com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized

import com.malinskiy.marathon.device.DeviceStub
import com.malinskiy.marathon.device.OperatingSystem
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class OperatingSystemVersionPoolingStrategySpek : Spek({
    describe("pooling strategy based on sdk version tests") {
        val strategy by memoized { OperatingSystemVersionPoolingStrategy() }
        it("should return DevicePoolId with name equals to device operating system version") {
            val operatingSystemVersionName = "27"
            val device = DeviceStub(
                operatingSystem = OperatingSystem(operatingSystemVersionName)
            )
            val poolId = strategy.associate(device)
            assertEquals(operatingSystemVersionName, poolId.name)
        }
    }
})
