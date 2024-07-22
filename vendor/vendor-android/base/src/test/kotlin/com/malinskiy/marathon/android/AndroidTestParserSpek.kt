package com.malinskiy.marathon.android

import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.File

class AndroidTestParserSpek : Spek(
    {
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
                    val extractedTests = runBlocking { parser.extract(componentInfo) }
                    extractedTests shouldBeEqualTo listOf(
                        Test(
                            "com.example", "MainActivityTest", "testText",
                            listOf(
                                MetaProperty("org.junit.Test"),
                                MetaProperty("kotlin.Metadata"),
                                MetaProperty("org.junit.runner.RunWith")
                            ),
                            componentInfo
                        )
                    )
                }
            }
        }
    })
