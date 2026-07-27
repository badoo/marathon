package com.malinskiy.marathon.android

import com.malinskiy.marathon.test.MetaProperty
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Index
import org.junit.jupiter.api.Test
import java.io.File
import com.malinskiy.marathon.test.Test as MarathonTest

/**
 * Source code of test apk is here
 * https://github.com/badoo/dex-test-parser/blob/main/test-app/src/androidTest/java/com/linkedin/parser/test/junit4/java/BasicJUnit4.java
 */
class AndroidTestParserTest {
    private val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
    private val componentInfo = stubAndroidComponentInfo(name = "", testApplicationOutput = apkFile)

    @Test
    fun `should return proper list of test methods`() = runTest {
        val parser = createAndroidTestParser()
        val extractedTests = parser.extract(componentInfo)

        assertThat(extractedTests)
            .hasSize(16)
            .contains(abstractTest(), Index.atIndex(0))
            .contains(basicJUnit4(), Index.atIndex(1))
            .contains(repeatableJUnit4(), Index.atIndex(5))
    }

    private fun TestScope.createAndroidTestParser(): AndroidTestParser =
        AndroidTestParser(StandardTestDispatcher(testScheduler))

    private fun abstractTest() = MarathonTest(
        pkg = "com.linkedin.parser.test.junit4.java", clazz = "BasicJUnit4", method = "abstractTest",
        metaProperties = listOf(
            MetaProperty(name = "org.junit.Test"),
            MetaProperty(name = "com.linkedin.parser.test.junit4.java.InheritedClassAnnotation"),
            MetaProperty(
                name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf("stringValue" to "Hello world!")
            )
        ), componentInfo = componentInfo
    )

    private fun basicJUnit4() = MarathonTest(
        pkg = "com.linkedin.parser.test.junit4.java", clazz = "BasicJUnit4", method = "basicJUnit4",
        metaProperties = listOf(
            MetaProperty(
                name = "om.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf("stringValue" to "Hello world!")
            ),
            MetaProperty(
                name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf(
                    "boolValue" to "true",
                    "enumValue" to "SUCCESS",
                    "intValue" to 12345,
                    "longValue" to 56789,
                    "stringValue" to "On a method"
                )
            ),
            MetaProperty(
                name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf("stringValue" to "Hello world!")
            )
        ), componentInfo = componentInfo
    )

    private fun repeatableJUnit4() = MarathonTest(
        pkg = "com.linkedin.parser.test.junit4.java", clazz = "BasicJUnit4", method = "repeatableJUnit4",
        metaProperties = listOf(
            MetaProperty(
                name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf("stringValue" to "Hello world!")
            ),
            MetaProperty(
                name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf("stringValue" to "On a method")
            ),
            MetaProperty(
                name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf("intValue" to "12345")
            ),
            MetaProperty(
                name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                values = mapOf("boolValue" to "true", "longValue" to 56789)
            ),
            MetaProperty(name = "org.junit.Test")
        ), componentInfo = componentInfo
    )
}
