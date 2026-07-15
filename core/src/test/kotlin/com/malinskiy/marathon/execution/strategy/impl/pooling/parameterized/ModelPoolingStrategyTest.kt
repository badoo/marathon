package com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized

import com.malinskiy.marathon.device.DeviceStub
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModelPoolingStrategyTest {
    private val strategy = ModelPoolingStrategy()

    @Test
    fun `should return DevicePoolId with name equals to device model`() {
        val model = "TestModel"
        val device = DeviceStub(model = model)
        val poolId = strategy.associate(device)

        assertThat(poolId.name).isEqualTo(model)
    }
}
