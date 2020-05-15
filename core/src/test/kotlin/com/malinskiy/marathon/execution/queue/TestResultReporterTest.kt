package com.malinskiy.marathon.execution.queue

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.createDeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.SimpleClassnameFilter
import com.malinskiy.marathon.execution.StrictRunFilterConfiguration
import com.malinskiy.marathon.execution.TestFilter
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.generateTest
import com.malinskiy.marathon.test.Mocks
import com.malinskiy.marathon.test.StubComponentCacheKeyProvider
import com.malinskiy.marathon.test.StubComponentInfoExtractor
import com.malinskiy.marathon.test.StubDeviceProvider
import com.malinskiy.marathon.test.TestVendorConfiguration
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.amshove.kluent.mock
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File

object TestResultReporterSpec : Spek(
    {
        val track: Track = mock()

        beforeEachTest {
            reset(track)
        }

        val defaultConfig = Configuration(
            name = "",
            outputDir = File(""),
            analyticsConfiguration = null,
            customAnalyticsTracker = null,
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
            debug = false,
            vendorConfiguration = TestVendorConfiguration(
                Mocks.TestParser.DEFAULT,
                StubDeviceProvider(),
                StubComponentInfoExtractor(),
                StubComponentCacheKeyProvider()
            ),
            analyticsTracking = false,
            pullScreenshotFilterConfiguration = null
        )
        val strictConfig = defaultConfig.copy(strictMode = true)
        val analytics = mock(Analytics::class)
        val test = generateTest()
        val poolId = DevicePoolId("test")

        fun filterDefault() = TestResultReporter(
            poolId,
            analytics,
            defaultConfig,
            track
        ).apply {
            addShard(TestShard(listOf(test, test, test)))
        }

        fun strictFilterReporter(filter: TestFilter) = TestResultReporter(
            poolId,
            analytics,
            defaultConfig.copy(strictRunFilterConfiguration = StrictRunFilterConfiguration(filter = listOf(filter), runs = 3)),
            track
        ).apply {
            addShard(TestShard(listOf(test, test, test)))
        }

        fun strictReporter() = TestResultReporter(
            poolId,
            analytics,
            strictConfig,
            track
        ).apply {
            addShard(TestShard(listOf(test, test, test)))
        }

        val deviceInfo = createDeviceInfo()

        afterEachTest {
            reset(analytics)
        }

        given("a reporter with a default config") {
            on("success - failure - failure") {
                it("should report success") {
                    val filter = filterDefault()

                    val r1 = TestResult(test, deviceInfo, TestStatus.PASSED, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.FAILURE, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.FAILURE, 4, 5, "test_batch_id")

                    filter.testFinished(deviceInfo, r1)
                    filter.testFailed(deviceInfo, r2)
                    filter.testFailed(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, true)
                        verify(track).test(poolId, deviceInfo, r2, false)
                        verify(track).test(poolId, deviceInfo, r3, false)
                        verifyNoMoreInteractions(track)
                    }
                }
            }

            on("failure - failure - success") {
                it("should report success") {
                    val filter = filterDefault()

                    val r1 = TestResult(test, deviceInfo, TestStatus.FAILURE, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.FAILURE, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.PASSED, 4, 5, "test_batch_id")

                    filter.testFailed(deviceInfo, r1)
                    filter.testFailed(deviceInfo, r2)
                    filter.testFinished(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, false)
                        verify(track).test(poolId, deviceInfo, r2, false)
                        verify(track).test(poolId, deviceInfo, r3, true)
                        verifyNoMoreInteractions(track)
                    }
                }
            }
        }

        given("a reporter with a strict config") {
            on("success - failure - failure") {
                it("should report failure") {
                    val reporter = strictReporter()

                    val r1 = TestResult(test, deviceInfo, TestStatus.PASSED, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.FAILURE, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.FAILURE, 4, 5, "test_batch_id")

                    reporter.testFinished(deviceInfo, r1)
                    reporter.testFailed(deviceInfo, r2)
                    reporter.testFailed(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, false)
                        verify(track).test(poolId, deviceInfo, r2, true)
                        verify(track).test(poolId, deviceInfo, r3, false)
                        verifyNoMoreInteractions(track)
                    }
                }
            }

            on("failure - success - success") {
                it("should report failure") {
                    val filter = strictReporter()

                    val r1 = TestResult(test, deviceInfo, TestStatus.FAILURE, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.PASSED, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.PASSED, 4, 5, "test_batch_id")

                    filter.testFailed(deviceInfo, r1)
                    filter.testFinished(deviceInfo, r2)
                    filter.testFinished(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, true)
                        verify(track).test(poolId, deviceInfo, r2, false)
                        verify(track).test(poolId, deviceInfo, r3, false)
                        verifyNoMoreInteractions(track)
                    }
                }
            }
        }

        given("a reporter with a strict run filter when test matches filter") {
            on("success - failure - failure") {
                it("should report failure") {
                    val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral(test.clazz)))

                    val r1 = TestResult(test, deviceInfo, TestStatus.PASSED, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.FAILURE, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.FAILURE, 4, 5, "test_batch_id")

                    reporter.testFinished(deviceInfo, r1)
                    reporter.testFailed(deviceInfo, r2)
                    reporter.testFailed(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, false)
                        verify(track).test(poolId, deviceInfo, r2, true)
                        verify(track).test(poolId, deviceInfo, r3, false)
                        verifyNoMoreInteractions(track)
                    }
                }
            }

            on("failure - success - success") {
                it("should report failure") {
                    val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral(test.clazz)))

                    val r1 = TestResult(test, deviceInfo, TestStatus.FAILURE, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.PASSED, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.PASSED, 4, 5, "test_batch_id")

                    reporter.testFailed(deviceInfo, r1)
                    reporter.testFinished(deviceInfo, r2)
                    reporter.testFinished(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, true)
                        verify(track).test(poolId, deviceInfo, r2, false)
                        verify(track).test(poolId, deviceInfo, r3, false)
                        verifyNoMoreInteractions(track)
                    }
                }
            }
        }

        given("a reporter with a strict run filter when test does not match filter") {
            on("success - failure - failure") {
                it("should report success") {
                    val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral("$^")))

                    val r1 = TestResult(test, deviceInfo, TestStatus.PASSED, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.FAILURE, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.FAILURE, 4, 5, "test_batch_id")

                    reporter.testFinished(deviceInfo, r1)
                    reporter.testFailed(deviceInfo, r2)
                    reporter.testFailed(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, true)
                        verify(track).test(poolId, deviceInfo, r2, false)
                        verify(track).test(poolId, deviceInfo, r3, false)
                        verifyNoMoreInteractions(track)
                    }
                }
            }

            on("failure - success - success") {
                it("should report success") {
                    val reporter = strictFilterReporter(filter = SimpleClassnameFilter(Regex.fromLiteral("$^")))

                    val r1 = TestResult(test, deviceInfo, TestStatus.FAILURE, 0, 1, "test_batch_id")
                    val r2 = TestResult(test, deviceInfo, TestStatus.PASSED, 2, 3, "test_batch_id")
                    val r3 = TestResult(test, deviceInfo, TestStatus.PASSED, 4, 5, "test_batch_id")

                    reporter.testFailed(deviceInfo, r1)
                    reporter.testFinished(deviceInfo, r2)
                    reporter.testFinished(deviceInfo, r3)

                    inOrder(track) {
                        verify(track).test(poolId, deviceInfo, r1, false)
                        verify(track).test(poolId, deviceInfo, r2, true)
                        verify(track).test(poolId, deviceInfo, r3, false)
                        verifyNoMoreInteractions(track)
                    }
                }
            }
        }
    })
