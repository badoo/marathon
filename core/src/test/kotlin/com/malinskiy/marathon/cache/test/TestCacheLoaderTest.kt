package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.cache.test.CacheResult.Hit
import com.malinskiy.marathon.cache.test.CacheResult.Miss
import com.malinskiy.marathon.cache.test.key.ComponentCacheKeyProvider
import com.malinskiy.marathon.cache.test.key.StubComponentCacheKeyProvider
import com.malinskiy.marathon.cache.test.key.TestCacheKeyFactory
import com.malinskiy.marathon.cache.test.key.VersionNameProvider
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.SimpleClassnameFilter
import com.malinskiy.marathon.execution.StrictRunConfiguration
import com.malinskiy.marathon.execution.StubComponentInfo
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.test.stubTests
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class TestCacheLoaderTest {
    private val cache = mock<TestResultsCache>()
    private val cacheKeyFactory = TestCacheKeyFactory(StubComponentCacheKeyProvider(), VersionNameProvider())
    private val poolId = DevicePoolId("pool")
    private val test = stubTest()
    private val results = mutableListOf<CacheResult>()

    @Test
    fun `GIVEN cache contains a test result WHEN checking a test THEN emits a cache hit with the cached result`() = runTest {
        val cachedResult = stubTestResult(test)
        whenever(cache.load(any(), eq(test))).thenReturn(cachedResult)
        val loader = createLoader()

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(test)))
        loader.stop()

        assertThat(results).containsExactly(Hit(poolId, cachedResult))
    }

    @Test
    fun `GIVEN cache does not contain a test result WHEN checking a test THEN emits a cache miss`() = runTest {
        whenever(cache.load(any(), eq(test))).thenReturn(null)
        val loader = createLoader()

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(test)))
        loader.stop()

        assertThat(results).containsExactly(Miss(poolId, test))
    }

    @Test
    fun `GIVEN mix of cached and uncached tests WHEN checking tests THEN emits a result for every test`() = runTest {
        val uncachedTest = stubTest(method = "uncached")
        val cachedTest = stubTest(method = "cached")
        val cachedResult = stubTestResult(cachedTest)
        whenever(cache.load(any(), eq(cachedTest))).thenReturn(cachedResult)
        val loader = createLoader()

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(uncachedTest, cachedTest)))
        loader.stop()

        assertThat(results).containsExactlyInAnyOrder(Miss(poolId, uncachedTest), Hit(poolId, cachedResult))
    }

    @Test
    fun `GIVEN multiple tests queued WHEN stopping THEN all queued tests are checked before stop completes`() = runTest {
        val tests = stubTests(10)
        val loader = createLoader()

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(tests))
        loader.stop()

        assertThat(results).containsExactlyInAnyOrderElementsOf(tests.map { Miss(poolId, it) })
    }

    @Test
    fun `GIVEN multiple tests queued WHEN checking tests THEN cache checks run concurrently`() = runTest {
        val tests = stubTests(8)
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        whenever(cache.load(any(), any())).doSuspendableAnswer {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            delay(100)
            inFlight.decrementAndGet()
            null
        }
        val loader = createLoader()

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(tests))
        loader.stop()

        assertThat(maxInFlight.get()).isEqualTo(8)
    }

    @Test
    fun `GIVEN more tests than the concurrency limit WHEN checking tests THEN concurrent checks do not exceed the limit`() = runTest {
        val tests = stubTests(8)
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        whenever(cache.load(any(), any())).doSuspendableAnswer {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            delay(100)
            inFlight.decrementAndGet()
            null
        }
        val loader = createLoader(fetchConcurrency = 2)

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(tests))
        loader.stop()

        assertThat(maxInFlight.get()).isEqualTo(2)
    }

    @Test
    fun `GIVEN concurrency of one WHEN checking tests THEN emits results in scheduling order`() = runTest {
        val tests = stubTests(10)
        val loader = createLoader(fetchConcurrency = 1)

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(tests))
        loader.stop()

        assertThat(results).containsExactlyElementsOf(tests.map { Miss(poolId, it) })
    }

    @Test
    fun `GIVEN non-positive concurrency WHEN creating the loader THEN fails`() {
        assertThatThrownBy { createLoader(fetchConcurrency = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN consumer throws WHEN consuming a result THEN loader terminates without hanging`() = runTest {
        val tests = stubTests(20)
        val loaderJob = Job()
        val propagatedExceptions = mutableListOf<Throwable>()
        val handler = CoroutineExceptionHandler { _, exception -> propagatedExceptions.add(exception) }
        val loader = createLoader()

        loader.start(CoroutineScope(coroutineContext + loaderJob + handler)) { throw IllegalStateException("Simulated consumer failure") }
        loader.addTests(poolId, TestShard(tests))
        loader.stop()

        assertThat(loaderJob.isCancelled).isTrue()
        assertThat(propagatedExceptions).singleElement().isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN concurrent checks WHEN consuming results THEN consumer is never invoked concurrently`() = runTest {
        val tests = stubTests(20)
        val inConsumer = AtomicInteger()
        val maxInConsumer = AtomicInteger()
        val loader = createLoader()

        loader.start(this) { result ->
            maxInConsumer.accumulateAndGet(inConsumer.incrementAndGet(), ::maxOf)
            delay(10)
            results.add(result)
            inConsumer.decrementAndGet()
        }
        loader.addTests(poolId, TestShard(tests))
        loader.stop()

        assertThat(maxInConsumer.get()).isEqualTo(1)
        assertThat(results).hasSize(20)
    }

    @Test
    fun `GIVEN test matches strict run filter WHEN checking a test THEN emits a cache miss without querying the cache`() = runTest {
        val strictRunConfiguration = StrictRunConfiguration(filter = listOf(SimpleClassnameFilter(Regex.fromLiteral(test.clazz))))
        val loader = createLoader(strictRunConfiguration = strictRunConfiguration)

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(test)))
        loader.stop()

        assertThat(results).containsExactly(Miss(poolId, test))
        verifyNoInteractions(cache)
    }

    @Test
    fun `GIVEN test does not match strict run filter WHEN checking a test THEN queries the cache`() = runTest {
        val strictRunConfiguration = StrictRunConfiguration(filter = listOf(SimpleClassnameFilter(Regex.fromLiteral("SomeOtherClazz"))))
        val cachedResult = stubTestResult(test)
        whenever(cache.load(any(), eq(test))).thenReturn(cachedResult)
        val loader = createLoader(strictRunConfiguration = strictRunConfiguration)

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(test)))
        loader.stop()

        assertThat(results).containsExactly(Hit(poolId, cachedResult))
    }

    @Test
    fun `GIVEN test result cached for another pool only WHEN checking the same test in two pools THEN emits hit only for the cached pool`() = runTest {
        val firstPool = DevicePoolId("first")
        val secondPool = DevicePoolId("second")
        val cachedResult = stubTestResult(test)
        whenever(cache.load(eq(cacheKeyFactory.getCacheKey(firstPool, test)), eq(test))).thenReturn(cachedResult)
        val loader = createLoader()

        loader.start(this) { results.add(it) }
        loader.addTests(firstPool, TestShard(listOf(test)))
        loader.addTests(secondPool, TestShard(listOf(test)))
        loader.stop()

        assertThat(results).containsExactlyInAnyOrder(Hit(firstPool, cachedResult), Miss(secondPool, test))
    }

    @Test
    fun `GIVEN cache load throws an exception WHEN checking tests THEN emits a miss and continues with subsequent tests`() = runTest {
        val failingTest = stubTest(method = "failing")
        val subsequentTest = stubTest(method = "subsequent")
        val subsequentResult = stubTestResult(subsequentTest)
        whenever(cache.load(any(), eq(failingTest))).thenThrow(RuntimeException("Simulated cache failure"))
        whenever(cache.load(any(), eq(subsequentTest))).thenReturn(subsequentResult)
        val loader = createLoader()

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(failingTest, subsequentTest)))
        loader.stop()

        assertThat(results).containsExactlyInAnyOrder(Miss(poolId, failingTest), Hit(poolId, subsequentResult))
    }

    @Test
    fun `GIVEN cache key computation throws an exception WHEN checking tests THEN emits a miss and continues with subsequent tests`() = runTest {
        val failingComponent = StubComponentInfo(name = "failing-component")
        val keyProvider = ComponentCacheKeyProvider { if (it == failingComponent) throw IOException("Simulated cache key failure") else it.name }
        val failingTest = stubTest(componentInfo = failingComponent, method = "failing")
        val subsequentTest = stubTest(method = "subsequent")
        val subsequentResult = stubTestResult(subsequentTest)
        whenever(cache.load(any(), eq(subsequentTest))).thenReturn(subsequentResult)
        val loader = createLoader(keyFactory = TestCacheKeyFactory(keyProvider, VersionNameProvider()))

        loader.start(this) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(failingTest, subsequentTest)))
        loader.stop()

        assertThat(results).containsExactlyInAnyOrder(Miss(poolId, failingTest), Hit(poolId, subsequentResult))
    }

    @Test
    fun `GIVEN cache load is in flight WHEN cancelling the loader scope THEN does not emit a result for the test`() = runTest {
        val loadStarted = CompletableDeferred<Unit>()
        whenever(cache.load(any(), eq(test))).doSuspendableAnswer {
            loadStarted.complete(Unit)
            awaitCancellation()
        }
        val loaderJob = Job()
        val loader = createLoader()

        loader.start(CoroutineScope(coroutineContext + loaderJob)) { results.add(it) }
        loader.addTests(poolId, TestShard(listOf(test)))
        loadStarted.await()
        loaderJob.cancelAndJoin()

        assertThat(results).isEmpty()
    }

    @Test
    fun `GIVEN loader was never started WHEN stopping THEN completes without emitting results`() = runTest {
        val loader = createLoader()

        loader.stop()

        assertThat(results).isEmpty()
    }

    private fun createLoader(
        keyFactory: TestCacheKeyFactory = cacheKeyFactory,
        strictRunConfiguration: StrictRunConfiguration = StrictRunConfiguration(),
        fetchConcurrency: Int = TestCacheLoader.DEFAULT_FETCH_CONCURRENCY,
    ): TestCacheLoader = TestCacheLoader(
        cache = cache,
        cacheKeyFactory = keyFactory,
        strictRunConfiguration = strictRunConfiguration,
        fetchConcurrency = fetchConcurrency,
    )
}
