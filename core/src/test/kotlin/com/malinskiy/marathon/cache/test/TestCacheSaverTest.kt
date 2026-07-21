package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.cache.test.key.ComponentCacheKeyProvider
import com.malinskiy.marathon.cache.test.key.StubComponentCacheKeyProvider
import com.malinskiy.marathon.cache.test.key.TestCacheKeyFactory
import com.malinskiy.marathon.cache.test.key.VersionNameProvider
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.StubComponentInfo
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class TestCacheSaverTest {
    private val cache = mock<TestResultsCache>()
    private val componentCacheKeyProvider = StubComponentCacheKeyProvider()
    private val versionNameProvider = VersionNameProvider()
    private val cacheKeyFactory = TestCacheKeyFactory(componentCacheKeyProvider, versionNameProvider)
    private val poolId = DevicePoolId("omni")
    private val test = stubTest()
    private val saver = TestCacheSaver(cache, cacheKeyFactory)

    @Test
    fun `WHEN saving a test result THEN stores it in the cache with the key of the test`() = runTest {
        val result = stubTestResult(test)
        val expectedKey = cacheKeyFactory.getCacheKey(poolId, test)

        saver.saveTestResult(poolId, result)
        saver.terminate()

        verify(cache).store(expectedKey, result)
    }

    @Test
    fun `WHEN saving multiple test results THEN stores all of them`() = runTest {
        val testResults = (1..5).map { stubTestResult(stubTest(method = "test$it")) }

        testResults.forEach { saver.saveTestResult(poolId, it) }
        saver.terminate()

        val storedResults = argumentCaptor<TestResult>()
        verify(cache, times(5)).store(any(), storedResults.capture())
        assertThat(storedResults.allValues).containsExactlyInAnyOrderElementsOf(testResults)
    }

    @Test
    fun `GIVEN a store is in flight WHEN terminating THEN waits for the store to complete`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var stored = false
        whenever(cache.store(any(), any())).doSuspendableAnswer {
            gate.await()
            stored = true
        }

        saver.saveTestResult(poolId, stubTestResult(test))
        launch { gate.complete(Unit) }
        saver.terminate()

        assertThat(stored).isTrue()
    }

    @Test
    fun `GIVEN cache key computation is cancelled for one result WHEN saving THEN still stores the other results`() = runTest {
        val failingComponent = StubComponentInfo(name = "failing-component")
        val keyProvider = ComponentCacheKeyProvider { if (it == failingComponent) throw CancellationException("Simulated cache key failure") else it.name }
        val saver = TestCacheSaver(cache, TestCacheKeyFactory(keyProvider, versionNameProvider))
        val failingResult = stubTestResult(stubTest(componentInfo = failingComponent))
        val storableResult = stubTestResult(test)

        saver.saveTestResult(poolId, failingResult)
        saver.saveTestResult(poolId, storableResult)
        saver.terminate()

        verify(cache).store(any(), eq(storableResult))
        verify(cache, never()).store(any(), eq(failingResult))
    }

    @Test
    fun `WHEN saving a test result after terminate THEN the result is not stored`() = runTest {
        saver.terminate()

        saver.saveTestResult(poolId, stubTestResult(test))

        verifyNoInteractions(cache)
    }

    @Test
    fun `GIVEN a store is in flight WHEN closing THEN the store is cancelled`() = runTest {
        val storeStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var stored = false
        whenever(cache.store(any(), any())).doSuspendableAnswer {
            storeStarted.complete(Unit)
            gate.await()
            stored = true
        }
        saver.saveTestResult(poolId, stubTestResult(test))
        storeStarted.await()

        saver.close()
        saver.terminate()

        assertThat(stored).isFalse()
    }
}
