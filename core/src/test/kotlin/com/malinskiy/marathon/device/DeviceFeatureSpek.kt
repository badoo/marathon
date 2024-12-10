package com.malinskiy.marathon.device

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows

class DeviceFeatureSpek : Spek({
    describe("DeviceFeature") {
        it("should parse valid uppercase string") {
            assertEquals(DeviceFeature.VIDEO, DeviceFeature.fromString("VIDEO"))
        }

        it("should parse valid lowercase string") {
            assertEquals(DeviceFeature.SCREENSHOT, DeviceFeature.fromString("screenshot"))
        }

        it("should return null for invalid string") {
            assertThrows<RuntimeException> { DeviceFeature.fromString("key let off") }
        }
    }
})
