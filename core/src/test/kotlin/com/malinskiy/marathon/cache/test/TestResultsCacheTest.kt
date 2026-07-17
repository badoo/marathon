package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.cache.MemoryCacheService
import com.malinskiy.marathon.cache.SimpleCacheKey
import com.malinskiy.marathon.createDeviceInfo
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.test.TestComponentInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.io.File
import com.malinskiy.marathon.test.Test as MarathonTest

class TestResultsCacheTest {
    @AutoClose
    private val cacheService = MemoryCacheService()

    @TempDir
    private lateinit var tempDir: File

    private val cache by lazy {
        val attachmentManager = AttachmentManager(tempDir)
        TestResultsCache(cacheService, attachmentManager, mock())
    }

    @Test
    fun `GIVEN empty cache WHEN loading a test result from cache THEN returns null`() = runTest {
        val test = createTest()
        val cacheKey = SimpleCacheKey("test")

        val result = cache.load(cacheKey, test)

        assertNull(result)
    }

    @Test
    fun `GIVEN cache with a test result WHEN loading the test result from cache THEN returns the original test result`() = runTest {
        val test = createTest()
        val deviceInfo = createDeviceInfo(
            deviceFeatures = listOf(DeviceFeature.SCREENSHOT, DeviceFeature.VIDEO)
        )
        val testResult = createTestResult(test, deviceInfo)
        val cacheKey = SimpleCacheKey("test")
        cache.store(cacheKey, testResult)

        val testResultFromCache = cache.load(cacheKey, test)

        assertNotNull(testResultFromCache)
        assertEquals(test, testResultFromCache!!.test)
        assertEquals(deviceInfo, testResultFromCache.device)
        assertEquals(TestStatus.PASSED, testResultFromCache.status)
        assertEquals(123, testResultFromCache.startTime)
        assertEquals(456, testResultFromCache.endTime)
        assertEquals("test_batch_id", testResultFromCache.batchId)
        assertEquals("stacktrace", testResultFromCache.stacktrace)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `GIVEN cache with a test result and attachment WHEN loading the test result from cache THEN returns the original test result with attachment`() = runTest {
        val tempFile = tempDir.resolve("attachment.txt").apply {
            writeText("abc")
        }

        val test = createTest()
        val testResult = createTestResult(test)
            .copy(attachments = listOf(Attachment(tempFile, AttachmentType.LOG, FileType.LOG)))
        val cacheKey = SimpleCacheKey("some-key")
        cache.store(cacheKey, testResult)

        val result = cache.load(cacheKey, test)

        assertNotNull(result)
        assertEquals(1, result!!.attachments.size)
        assertEquals("abc", result.attachments.first().file.readText())
        assertEquals(AttachmentType.LOG, result.attachments.first().type)
        assertEquals(FileType.LOG, result.attachments.first().fileType)
    }

    @Test
    fun `GIVEN cache service throws an exception on load WHEN loading a test result THEN returns null`() = runTest {
        val test = createTest()
        val testResult = createTestResult(test)
        val cacheKey = SimpleCacheKey("test")
        cache.store(cacheKey, testResult)
        cacheService.throwExceptions()

        val result = cache.load(cacheKey, testResult.test)

        assertNull(result)
    }

    @Test
    fun `GIVEN cache service throws an exception on store WHEN storing a test result THEN doesn't throw an exception`() = runTest {
        cacheService.throwExceptions()
        val test = createTest()
        val testResult = createTestResult(test)
        val cacheKey = SimpleCacheKey("test")

        cache.store(cacheKey, testResult)
    }

    @Test
    fun `GIVEN cache service throws CancellationException WHEN loading a test result THEN returns null`() = runTest {
        val test = createTest()
        val cacheKey = SimpleCacheKey("test")
        cacheService.throwExceptions(CancellationException("Cancellation from the cache service"))

        val result = cache.load(cacheKey, test)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN cache service throws CancellationException WHEN storing a test result THEN doesn't throw an exception`() = runTest {
        cacheService.throwExceptions(CancellationException("Cancellation from the cache service"))
        val test = createTest()
        val testResult = createTestResult(test)
        val cacheKey = SimpleCacheKey("test")

        cache.store(cacheKey, testResult)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `GIVEN calling coroutine is cancelled AND cache service throws CancellationException WHEN loading a test result THEN rethrows the cancellation`() = runTest {
        val test = createTest()
        val cacheKey = SimpleCacheKey("test")
        cacheService.throwExceptions(CancellationException("Cancellation from the cache service"))
        var loadReturned = false

        val job = launch {
            cancel()
            cache.load(cacheKey, test)
            loadReturned = true
        }
        job.join()

        assertThat(loadReturned).isFalse()
    }

    @Test
    @Suppress("MaxLineLength")
    fun `GIVEN calling coroutine is cancelled AND cache service throws CancellationException WHEN storing a test result THEN rethrows the cancellation`() = runTest {
        cacheService.throwExceptions(CancellationException("Cancellation from the cache service"))
        val test = createTest()
        val testResult = createTestResult(test)
        val cacheKey = SimpleCacheKey("test")
        var storeReturned = false

        val job = launch {
            cancel()
            cache.store(cacheKey, testResult)
            storeReturned = true
        }
        job.join()

        assertThat(storeReturned).isFalse()
    }

    @Test
    fun `GIVEN calling coroutine is cancelled AND cache service throws an exception WHEN loading a test result THEN rethrows the cancellation`() = runTest {
        val test = createTest()
        val cacheKey = SimpleCacheKey("test")
        cacheService.throwExceptions(RuntimeException("Exception from the cache service"))
        var loadReturned = false

        val job = launch {
            cancel()
            cache.load(cacheKey, test)
            loadReturned = true
        }
        job.join()

        assertThat(loadReturned).isFalse()
    }

    @Test
    fun `GIVEN calling coroutine is cancelled AND cache service throws an exception WHEN storing a test result THEN rethrows the cancellation`() = runTest {
        cacheService.throwExceptions(RuntimeException("Exception from the cache service"))
        val test = createTest()
        val testResult = createTestResult(test)
        val cacheKey = SimpleCacheKey("test")
        var storeReturned = false

        val job = launch {
            cancel()
            cache.store(cacheKey, testResult)
            storeReturned = true
        }
        job.join()

        assertThat(storeReturned).isFalse()
    }

    private fun createTest(): MarathonTest =
        MarathonTest(
            pkg = "com.test",
            clazz = "Test",
            method = "test1",
            componentInfo = TestComponentInfo(someInfo = "someInfo", name = "component-name"),
            metaProperties = emptyList()
        )

    private fun createTestResult(
        test: MarathonTest,
        deviceInfo: DeviceInfo = createDeviceInfo(),
    ): TestResult = TestResult(
        test = test,
        device = deviceInfo,
        status = TestStatus.PASSED,
        startTime = 123,
        endTime = 456,
        batchId = "test_batch_id",
        stacktrace = "stacktrace"
    )
}
