package com.malinskiy.marathon.android

import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Index
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.File

/**
 * Source code of test apk is here
 * https://github.com/badoo/dex-test-parser/blob/main/test-app/src/androidTest/java/com/linkedin/parser/test/junit4/java/BasicJUnit4.java
 */
class AndroidTestParserSpek : Spek({
    describe("android test parser") {
        val parser = AndroidTestParser()

        group("android test apk") {
            val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
            val componentInfo = AndroidComponentInfo(
                name = "",
                applicationId = null,
                testApplicationId = "com.example.test",
                applicationOutput = null,
                testApplicationOutput = apkFile
            )

            it("should return proper list of test methods") {
                runTest {
                    val extractedTests = parser.extract(componentInfo)
                    assertThat(extractedTests)
                        .hasSize(16)
                        .contains(
                            Test(
                                pkg = "com.linkedin.parser.test.junit4.java", clazz = "BasicJUnit4", method = "abstractTest",
                                metaProperties = listOf(
                                    MetaProperty(name = "org.junit.Test"),
                                    MetaProperty(name = "com.linkedin.parser.test.junit4.java.InheritedClassAnnotation"),
                                    MetaProperty(
                                        name = "com.linkedin.parser.test.junit4.java.TestValueAnnotation",
                                        values = mapOf("stringValue" to "Hello world!")
                                    )
                                ), componentInfo = componentInfo
                            ), Index.atIndex(0)
                        ).contains(
                            Test(
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
                            ), Index.atIndex(1)
                        ).contains(
                            Test(
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
                            ), Index.atIndex(5)
                        )
                }
            }
        }
    }
})
