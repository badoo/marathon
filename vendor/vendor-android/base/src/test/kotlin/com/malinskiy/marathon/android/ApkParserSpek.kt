package com.malinskiy.marathon.android

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File

/**
 * Source code of test apk is here
 * https://github.com/badoo/dex-test-parser/blob/main/test-app/src/androidTest/java/com/linkedin/parser/test/junit4/java/BasicJUnit4.java
 */
class ApkParserSpek : Spek({
    describe("apk parser") {
        it("should parser AndroidManifest and return InstrumentationInfo") {
            val parser = ApkParser()
            val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
            val instrumentationInfo = parser.parseInstrumentationInfo(apkFile)
            assertEquals(
                InstrumentationInfo(
                    "com.linkedin.parser.test",
                    "com.linkedin.parser.test.test",
                    "android.test.InstrumentationTestRunner"
                ),
                instrumentationInfo
            )
        }
    }
})
