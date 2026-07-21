package com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized

import com.malinskiy.marathon.device.StubDevice
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AbiPoolingStrategyTest {
    private val strategy = AbiPoolingStrategy()

    @Test
    fun `should return DevicePoolId with name equals to device abi`() {
        val abi = "Test_ABI"
        val device = StubDevice(abi = abi)
        val poolId = strategy.associate(device)

        assertThat(poolId.name).isEqualTo(abi)
    }
}
