package com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized

import com.malinskiy.marathon.device.DeviceStub
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals

class ModelPoolingStrategySpek : Spek({
    describe("pooling strategy based on device model tests") {
        val strategy by memoized { ModelPoolingStrategy() }
        it("should return DevicePoolId with name equals to device model") {
            val model = "TestModel"
            val device = DeviceStub(model = model)
            val poolId = strategy.associate(device)
            assertEquals(model, poolId.name)
        }
    }
})
