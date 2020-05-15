package com.malinskiy.marathon.android

import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File

class AndroidTestParserSpek : Spek(
    {
        describe("android test parser") {
            val parser = AndroidTestParser()

            on("android test apk") {
                val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
                val configuration = Configuration(
                    name = "",
                    outputDir = File(""),
                    customAnalyticsTracker = null,
                    analyticsConfiguration = null,
                    poolingStrategy = null,
                    shardingStrategy = null,
                    sortingStrategy = null,
                    batchingStrategy = null,
                    flakinessStrategy = null,
                    retryStrategy = null,
                    filteringConfiguration = null,
                    strictRunFilterConfiguration = null,
                    cache = null,
                    ignoreFailures = null,
                    isCodeCoverageEnabled = null,
                    fallbackToScreenshots = null,
                    strictMode = null,
                    uncompletedTestRetryQuota = null,
                    testClassRegexes = null,
                    includeSerialRegexes = null,
                    excludeSerialRegexes = null,
                    testBatchTimeoutMillis = null,
                    testOutputTimeoutMillis = null,
                    debug = null,
                    vendorConfiguration = AndroidConfiguration(
                        implementationModules = emptyList(),
                        androidSdk = File(""),
                        applicationOutput = File(""),
                        testApplicationOutput = apkFile
                    ),
                    analyticsTracking = false,
                    pullScreenshotFilterConfiguration = null
                )
                val componentInfo = AndroidComponentInfoExtractor().extract(configuration)

                it("should return proper list of test methods") {
                    val extractedTests = runBlocking { parser.extract(componentInfo) }
                    extractedTests shouldEqual listOf(
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

