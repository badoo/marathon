package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.cache.MemoryCacheService
import com.malinskiy.marathon.cache.SimpleCacheKey
import com.malinskiy.marathon.createDeviceInfo
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestComponentInfo
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.mock
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.File
import java.nio.file.Files

class TestResultsCacheSpek : Spek(
    {
        val cacheService by memoized {
            MemoryCacheService()
        }

        val cache by memoized {
            val attachmentManager = AttachmentManager(Files.createTempDirectory("test_output").toFile())
            TestResultsCache(cacheService, attachmentManager, mock())
        }

        describe("TestResultsCache") {
            it("should return null when load test with empty cache") {
                runBlocking {
                    val result = cache.load(SimpleCacheKey("test"), createTest())

                    result shouldBeEqualTo null
                }
            }

            it("should return saved test result when load after saving") {
                runBlocking {
                    val test = Test(
                        pkg = "com.test",
                        clazz = "Test",
                        method = "test1",
                        componentInfo = TestComponentInfo(someInfo = "someInfo", name = "component-name"),
                        metaProperties = emptyList()
                    )
                    val deviceInfo = createDeviceInfo(
                        deviceFeatures = listOf(DeviceFeature.SCREENSHOT, DeviceFeature.VIDEO)
                    )
                    val testResult = TestResult(
                        test = test,
                        device = deviceInfo,
                        status = TestStatus.PASSED,
                        startTime = 123,
                        endTime = 456,
                        batchId = "batch_id",
                        stacktrace = "stacktrace"
                    )

                    cache.store(SimpleCacheKey("test"), testResult)
                    val testResultFromCache = cache.load(SimpleCacheKey("test"), test)

                    testResultFromCache shouldNotBeEqualTo null
                    testResultFromCache!!.test shouldBeEqualTo test
                    testResultFromCache.device shouldBeEqualTo deviceInfo
                    testResultFromCache.status shouldBeEqualTo TestStatus.PASSED
                    testResultFromCache.startTime shouldBeEqualTo 123
                    testResultFromCache.endTime shouldBeEqualTo 456
                    testResultFromCache.batchId shouldBeEqualTo "batch_id"
                    testResultFromCache.stacktrace shouldBeEqualTo "stacktrace"
                }
            }

            it("should return saved test result when load after saving with attachment") {
                val tempFile = File.createTempFile("test", "123").apply {
                    writeText("abc")
                    deleteOnExit()
                }

                runBlocking {
                    val test = createTest()
                    val testResult = createTestResult(
                        attachments = listOf(Attachment(tempFile, AttachmentType.LOG, FileType.LOG))
                    )

                    cache.store(SimpleCacheKey("some-key"), testResult)
                    val result = cache.load(SimpleCacheKey("some-key"), test)

                    result shouldNotBeEqualTo null
                    result!!.attachments.size shouldBeEqualTo 1
                    result.attachments.first().file.readText() shouldBeEqualTo "abc"
                    result.attachments.first().type shouldBeEqualTo AttachmentType.LOG
                    result.attachments.first().fileType shouldBeEqualTo FileType.LOG
                }
            }

            it("should return null when exception occurred during reading") {
                val tempFile = File.createTempFile("test", "123").apply {
                    writeText("abc")
                    deleteOnExit()
                }

                runBlocking {
                    val testResult = createTestResult()
                    cache.store(SimpleCacheKey("test"), testResult)
                    cacheService.throwExceptions()

                    val result = cache.load(SimpleCacheKey("test"), testResult.test)

                    result shouldBeEqualTo null
                }
            }

            it("should not fail when error occurred during writing") {
                runBlocking {
                    cacheService.throwExceptions()
                    val testResult = createTestResult()

                    cache.store(SimpleCacheKey("test"), testResult)
                }
            }
        }
    })

private fun createTestResult(attachments: List<Attachment> = emptyList()) = TestResult(
    test = createTest(),
    device = createDeviceInfo(),
    status = TestStatus.PASSED,
    startTime = 123,
    endTime = 456,
    attachments = attachments,
    batchId = "test_batch_id"
)

private fun createTest() = Test(
    pkg = "com.test",
    clazz = "Test",
    method = "test1",
    componentInfo = TestComponentInfo(someInfo = "someInfo", name = "component-name"),
    metaProperties = emptyList()
)
