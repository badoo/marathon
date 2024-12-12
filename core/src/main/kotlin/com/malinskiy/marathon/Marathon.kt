package com.malinskiy.marathon

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.analytics.internal.sub.TrackerInternal
import com.malinskiy.marathon.cache.CacheService
import com.malinskiy.marathon.cache.test.CacheTestReporter
import com.malinskiy.marathon.cache.test.TestCacheLoader
import com.malinskiy.marathon.cache.test.TestCacheSaver
import com.malinskiy.marathon.config.LogicalConfigurationValidator
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.exceptions.ReportGenerationException
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.Scheduler
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.execution.StrictRunProcessor
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toTestName
import com.malinskiy.marathon.time.Timer
import kotlin.coroutines.coroutineContext

class Marathon(
    val configuration: Configuration,
    private val deviceProvider: DeviceProvider,
    private val tracker: TrackerInternal,
    private val analytics: Analytics,
    private val cacheService: CacheService,
    private val testCacheLoader: TestCacheLoader,
    private val testCacheSaver: TestCacheSaver,
    private val testParser: TestParser,
    private val cachedTestsReporter: CacheTestReporter,
    private val progressReporter: ProgressReporter,
    private val strictRunChecker: StrictRunChecker,
    private val logsProvider: LogsProvider,
    private val track: Track,
    private val timer: Timer
) : MarathonRunner {

    private val logger = MarathonLogging.getLogger(Marathon::class.java)

    private val configurationValidator = LogicalConfigurationValidator()
    private val strictRunProcessor = StrictRunProcessor(configuration.strictRunConfiguration)

    private lateinit var scheduler: Scheduler

    override suspend fun start() {
        logger.debug("Starting Marathon")

        configurationValidator.validate(configuration)

        logger.debug("Initializing device provider")
        deviceProvider.initialize()

        if (configuration.outputDir.exists()) {
            logger.info("Cleaning output directory ${configuration.outputDir}")
            configuration.outputDir.deleteRecursively()
        }
        configuration.outputDir.mkdirs()

        logger.debug("Initializing scheduler")
        val currentCoroutineContext = coroutineContext
        scheduler = Scheduler(
            deviceProvider,
            testCacheLoader,
            testCacheSaver,
            cachedTestsReporter,
            analytics,
            configuration,
            progressReporter,
            strictRunChecker,
            logsProvider,
            track,
            timer,
            currentCoroutineContext
        )
        scheduler.initialize()
    }

    override suspend fun scheduleTests(componentInfo: ComponentInfo) {
        val parsedTests = testParser.extract(componentInfo)
        if (parsedTests.isEmpty()) return

        val tests = applyTestFilters(parsedTests)
        logger.info("Scheduling {} tests for {} component", tests.size, componentInfo.name)
        logger.info(tests.joinToString(", ") { it.toTestName() })

        val shard = prepareTestShard(tests, analytics)
        scheduler.addTests(shard)
    }

    override suspend fun stopAndWaitForCompletion(): Boolean {
        logger.debug("Waiting for test run to complete")

        try {
            scheduler.stopAndWaitForCompletion()
            onFinish()
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            // We don't want to catch these. If an exception was thrown, we should fail the execution
            logger.error("An error occurred while finishing test run", e)
            throw e
        }
        return progressReporter.aggregateResult()
    }

    override fun close() {
        deviceProvider.close()
        cacheService.close()
        analytics.close()
    }

    private suspend fun onFinish() {
        deviceProvider.terminate()
        try {
            tracker.finish()
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            throw ReportGenerationException("Failed to generate test run report", e)
        }
    }

    private fun applyTestFilters(parsedTests: List<Test>): List<Test> {
        var tests = parsedTests.filter { test ->
            configuration.testClassRegexes.all { it.matches(test.clazz) }
        }
        configuration.filteringConfiguration.whitelist.forEach { tests = it.filter(tests) }
        configuration.filteringConfiguration.blacklist.forEach { tests = it.filterNot(tests) }
        return tests
    }

    private fun prepareTestShard(tests: List<Test>, analytics: Analytics): TestShard {
        val shardingStrategy = configuration.shardingStrategy
        val flakinessStrategy = configuration.flakinessStrategy
        val shard = shardingStrategy.createShard(tests)
        val flakinessShard = flakinessStrategy.process(shard, analytics)
        return strictRunProcessor.processShard(flakinessShard)
    }
}
