package com.malinskiy.marathon.cache.test

import com.malinskiy.marathon.cache.test.CacheResult.Hit
import com.malinskiy.marathon.cache.test.CacheResult.Miss
import com.malinskiy.marathon.cache.test.key.StubComponentCacheKeyProvider
import com.malinskiy.marathon.cache.test.key.TestCacheKeyFactory
import com.malinskiy.marathon.cache.test.key.VersionNameProvider
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.StrictRunConfiguration
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.test.stubTests
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import com.malinskiy.marathon.test.Test as MarathonTest

/**
 * Randomized concurrency tests for [TestCacheLoader]: random mixes of hits, misses, failures,
 * per-load delays and concurrency limits, checked against the loader's invariants.
 */
class TestCacheLoaderFuzzTest {
    private val cacheKeyFactory = TestCacheKeyFactory(StubComponentCacheKeyProvider(), VersionNameProvider())
    private val poolId = DevicePoolId("pool")

    @RepeatedTest(value = VIRTUAL_TIME_ITERATIONS, failureThreshold = 1)
    fun `GIVEN random hits misses failures and delays WHEN checking tests THEN emits exactly one correct result per test`(repetitionInfo: RepetitionInfo) =
        runTest {
            val random = Random(repetitionInfo.currentRepetition.toLong())
            val scenario = Scenario(random = random)
            val results = mutableListOf<CacheResult>()
            val inConsumer = AtomicInteger()
            val maxInConsumer = AtomicInteger()
            val maxInFlight = AtomicInteger()
            val fetchConcurrency = random.nextInt(1, 33)
            val loader = createLoader(scenario = scenario, fetchConcurrency = fetchConcurrency, maxInFlight = maxInFlight)

            loader.start(this) { result ->
                maxInConsumer.accumulateAndGet(inConsumer.incrementAndGet(), ::maxOf)
                delay(scenario.consumerDelay)
                results.add(result)
                inConsumer.decrementAndGet()
            }
            scenario.tests.chunked(random.nextInt(1, 65)).forEach { chunk ->
                loader.addTests(poolId, TestShard(chunk))
                delay(random.nextLong(0, 10).milliseconds)
            }
            loader.stop()

            assertThat(results).containsExactlyInAnyOrderElementsOf(scenario.expectedResults(poolId))
            assertThat(maxInConsumer.get()).describedAs("consumer reentrancy").isLessThanOrEqualTo(1)
            assertThat(maxInFlight.get()).describedAs("concurrency bound").isLessThanOrEqualTo(fetchConcurrency)
        }

    @RepeatedTest(value = REAL_DISPATCHER_ITERATIONS, failureThreshold = 1)
    fun `GIVEN a real multi-threaded dispatcher WHEN checking tests THEN emits exactly one correct result per test`(repetitionInfo: RepetitionInfo) =
        runTest(timeout = 5.minutes) {
            val random = Random(1000L + repetitionInfo.currentRepetition)
            val scenario = Scenario(random = random, maxTestCount = 128, maxLoadDelay = 3.milliseconds, consumerDelay = Duration.ZERO)
            val results = ConcurrentLinkedQueue<CacheResult>()
            val inConsumer = AtomicInteger()
            val maxInConsumer = AtomicInteger()
            val maxInFlight = AtomicInteger()
            val fetchConcurrency = random.nextInt(1, 33)
            val loader = createLoader(scenario = scenario, fetchConcurrency = fetchConcurrency, maxInFlight = maxInFlight)

            withContext(Dispatchers.Default) {
                coroutineScope {
                    loader.start(this) { result ->
                        maxInConsumer.accumulateAndGet(inConsumer.incrementAndGet(), ::maxOf)
                        results.add(result)
                        inConsumer.decrementAndGet()
                    }
                    loader.addTests(poolId, TestShard(scenario.tests))
                    loader.stop()
                }
            }

            assertThat(results).containsExactlyInAnyOrderElementsOf(scenario.expectedResults(poolId))
            assertThat(maxInConsumer.get()).describedAs("consumer reentrancy").isLessThanOrEqualTo(1)
            assertThat(maxInFlight.get()).describedAs("concurrency bound").isLessThanOrEqualTo(fetchConcurrency)
        }

    @RepeatedTest(value = VIRTUAL_TIME_ITERATIONS, failureThreshold = 1)
    fun `GIVEN cancellation at a random point WHEN checking tests THEN never emits duplicate or phantom results`(repetitionInfo: RepetitionInfo) = runTest {
        val random = Random(2000L + repetitionInfo.currentRepetition)
        val scenario = Scenario(random = random, minLoadDelay = 1.milliseconds, maxLoadDelay = 20.milliseconds)
        val results = mutableListOf<CacheResult>()
        val loaderJob = Job()
        val loader = createLoader(scenario = scenario, fetchConcurrency = random.nextInt(1, 33), maxInFlight = AtomicInteger())

        loader.start(CoroutineScope(coroutineContext + loaderJob)) { results.add(it) }
        loader.addTests(poolId, TestShard(scenario.tests))
        delay(random.nextLong(0, 200).milliseconds)
        loaderJob.cancelAndJoin()
        loader.close()

        assertThat(results).doesNotHaveDuplicates()
        assertThat(results).isSubsetOf(scenario.expectedResults(poolId))
    }

    private suspend fun createLoader(scenario: Scenario, fetchConcurrency: Int, maxInFlight: AtomicInteger): TestCacheLoader {
        val cache = mock<TestResultsCache>()
        val inFlight = AtomicInteger()
        whenever(cache.load(any(), any())).doSuspendableAnswer { invocation ->
            val test = invocation.getArgument<MarathonTest>(1)
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            try {
                delay(scenario.loadDelays.getValue(test))
                when (val outcome = scenario.outcomes.getValue(test)) {
                    is Outcome.Hit -> outcome.testResult
                    is Outcome.Miss -> null
                    is Outcome.Failure -> throw IOException("Simulated cache failure for $test")
                }
            } finally {
                inFlight.decrementAndGet()
            }
        }
        return TestCacheLoader(
            cache = cache,
            cacheKeyFactory = cacheKeyFactory,
            strictRunConfiguration = StrictRunConfiguration(),
            fetchConcurrency = fetchConcurrency,
        )
    }

    private class Scenario(
        random: Random,
        maxTestCount: Int = 64,
        minLoadDelay: Duration = 0.milliseconds,
        maxLoadDelay: Duration = 10.milliseconds,
        val consumerDelay: Duration = random.nextLong(0, 5).milliseconds,
    ) {
        val tests: List<MarathonTest> = stubTests(random.nextInt(0, maxTestCount + 1))
        val outcomes: Map<MarathonTest, Outcome> = tests.associateWith { test ->
            when (random.nextInt(3)) {
                0 -> Outcome.Hit(stubTestResult(test))
                1 -> Outcome.Miss
                else -> Outcome.Failure
            }
        }
        val loadDelays: Map<MarathonTest, Duration> = tests.associateWith {
            random.nextLong(minLoadDelay.inWholeMilliseconds, maxLoadDelay.inWholeMilliseconds + 1).milliseconds
        }

        fun expectedResults(poolId: DevicePoolId): List<CacheResult> = tests.map { test ->
            when (val outcome = outcomes.getValue(test)) {
                is Outcome.Hit -> Hit(poolId, outcome.testResult)
                is Outcome.Miss, is Outcome.Failure -> Miss(poolId, test)
            }
        }
    }

    private sealed class Outcome {
        data class Hit(val testResult: TestResult) : Outcome()
        data object Miss : Outcome()
        data object Failure : Outcome()
    }

    private companion object {
        private const val VIRTUAL_TIME_ITERATIONS = 200
        private const val REAL_DISPATCHER_ITERATIONS = 25
    }
}
