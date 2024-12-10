package com.malinskiy.marathon.device

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class DeviceFeatureSpek : Spek({
    describe("DeviceFeature") {
        it("should parse valid uppercase string") {
            DeviceFeature.fromString("VIDEO") shouldBeEqualTo DeviceFeature.VIDEO
        }

        it("should parse valid lowercase string") {
            DeviceFeature.fromString("screenshot") shouldBeEqualTo DeviceFeature.SCREENSHOT
        }

        it("should return null for invalid string") {
            { DeviceFeature.fromString("key let off") } shouldThrow RuntimeException::class
        }
    }
})
