package com.malinskiy.marathon.android

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Source code of test apk is here
 * https://github.com/badoo/dex-test-parser/blob/main/test-app/src/androidTest/java/com/linkedin/parser/test/junit4/java/BasicJUnit4.java
 */
class ApkParserTest {
    @Test
    fun `should parse AndroidManifest and return InstrumentationInfo`() {
        val parser = ApkParser()
        val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
        val instrumentationInfo = parser.parseInstrumentationInfo(apkFile)

        assertThat(instrumentationInfo).isEqualTo(
            InstrumentationInfo(
                applicationPackage = "com.linkedin.parser.test",
                instrumentationPackage = "com.linkedin.parser.test.test",
                testRunnerClass = "android.test.InstrumentationTestRunner",
            ),
        )
    }
}
