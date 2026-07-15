package com.malinskiy.marathon.device

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DeviceFeatureTest {
    @Test
    fun `should parse valid uppercase string`() {
        val feature = DeviceFeature.fromString("VIDEO")

        assertThat(feature).isEqualTo(DeviceFeature.VIDEO)
    }

    @Test
    fun `should parse valid lowercase string`() {
        val feature = DeviceFeature.fromString("screenshot")

        assertThat(feature).isEqualTo(DeviceFeature.SCREENSHOT)
    }

    @Test
    fun `should throw for invalid string`() {
        assertThatThrownBy { DeviceFeature.fromString("key let off") }
            .isInstanceOf(RuntimeException::class.java)
    }
}
