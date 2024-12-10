package com.malinskiy.marathon.android

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File

class ApkParserSpek : Spek({
    describe("apk parser") {
        it("should parser AndroidManifest and return InstrumentationInfo") {
            val parser = ApkParser()
            val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
            val instrumentationInfo = parser.parseInstrumentationInfo(apkFile)
            assertEquals(
                InstrumentationInfo(
                    "com.example",
                    "com.example.test",
                    "android.support.test.runner.AndroidJUnitRunner"
                ),
                instrumentationInfo
            )
        }
    }
})
