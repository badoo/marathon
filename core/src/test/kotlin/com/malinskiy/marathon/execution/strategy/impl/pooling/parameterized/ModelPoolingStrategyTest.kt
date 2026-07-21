package com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized

import com.malinskiy.marathon.device.StubDevice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModelPoolingStrategyTest {
    private val strategy = ModelPoolingStrategy()

    @Test
    fun `should return DevicePoolId with name equals to device model`() {
        val model = "TestModel"
        val device = StubDevice(model = model)
        val poolId = strategy.associate(device)

        assertThat(poolId.name).isEqualTo(model)
    }
}
