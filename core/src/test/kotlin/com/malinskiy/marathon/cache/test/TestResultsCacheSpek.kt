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
import kotlinx.coroutines.test.runTest
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.mockito.kotlin.mock
import java.io.File
import java.nio.file.Files

class TestResultsCacheSpek : Spek({
    val cacheService by memoized {
        MemoryCacheService()
    }

    val cache by memoized {
        val attachmentManager = AttachmentManager(Files.createTempDirectory("test_output").toFile())
        TestResultsCache(cacheService, attachmentManager, mock())
    }

    describe("TestResultsCache") {
        it("should return null when load test with empty cache") {
            runTest {
                val result = cache.load(SimpleCacheKey("test"), createTest())

                assertNull(result)
            }
        }

        it("should return saved test result when load after saving") {
            runTest {
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

                assertNotNull(testResultFromCache)
                assertEquals(test, testResultFromCache!!.test)
                assertEquals(deviceInfo, testResultFromCache.device)
                assertEquals(TestStatus.PASSED, testResultFromCache.status)
                assertEquals(123, testResultFromCache.startTime)
                assertEquals(456, testResultFromCache.endTime)
                assertEquals("batch_id", testResultFromCache.batchId)
                assertEquals("stacktrace", testResultFromCache.stacktrace)
            }
        }

        it("should return saved test result when load after saving with attachment") {
            val tempFile = File.createTempFile("test", "123").apply {
                writeText("abc")
                deleteOnExit()
            }

            runTest {
                val test = createTest()
                val testResult = createTestResult(
                    attachments = listOf(Attachment(tempFile, AttachmentType.LOG, FileType.LOG))
                )

                cache.store(SimpleCacheKey("some-key"), testResult)
                val result = cache.load(SimpleCacheKey("some-key"), test)

                assertNotNull(result)
                assertEquals(1, result!!.attachments.size)
                assertEquals("abc", result.attachments.first().file.readText())
                assertEquals(AttachmentType.LOG, result.attachments.first().type)
                assertEquals(FileType.LOG, result.attachments.first().fileType)
            }
        }

        it("should return null when exception occurred during reading") {
            runTest {
                val testResult = createTestResult()
                cache.store(SimpleCacheKey("test"), testResult)
                cacheService.throwExceptions()

                val result = cache.load(SimpleCacheKey("test"), testResult.test)

                assertNull(result)
            }
        }

        it("should not fail when error occurred during writing") {
            runTest {
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
