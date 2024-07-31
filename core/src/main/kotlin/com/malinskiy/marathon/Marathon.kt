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
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.log.MarathonLogConfigurator
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toTestName
import com.malinskiy.marathon.time.Timer
import com.malinskiy.marathon.vendor.VendorConfiguration
import kotlinx.coroutines.runBlocking
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
    private val attachmentManager: AttachmentManager,
    private val strictRunChecker: StrictRunChecker,
    private val logConfigurator: MarathonLogConfigurator,
    private val logsProvider: LogsProvider,
    private val track: Track,
    private val timer: Timer
) : MarathonRunner {

    private val logger = MarathonLogging.logger("Marathon")

    private val configurationValidator = LogicalConfigurationValidator()
    private val strictRunProcessor = StrictRunProcessor(configuration.strictRunConfiguration)

    private lateinit var scheduler: Scheduler
    private lateinit var hook: ShutdownHook

    private fun configureLogging(vendorConfiguration: VendorConfiguration) {
        MarathonLogging.debug = configuration.debug

        logConfigurator.configure(vendorConfiguration)
    }

    override suspend fun start() {
        configureLogging(configuration.vendorConfiguration)

        deviceProvider.initialize(configuration.vendorConfiguration)
        logger.debug { "Finished loading device provider" }

        configurationValidator.validate(configuration)

        val currentCoroutineContext = coroutineContext
        scheduler = Scheduler(
            deviceProvider,
            cacheService,
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

        logger.debug { "Created scheduler" }

        if (configuration.outputDir.exists()) {
            logger.info { "Output ${configuration.outputDir} already exists" }
            configuration.outputDir.deleteRecursively()
        }
        configuration.outputDir.mkdirs()

        hook = installShutdownHook { onFinish(analytics, deviceProvider, attachmentManager) }

        scheduler.initialize()
    }

    override suspend fun scheduleTests(componentInfo: ComponentInfo) {
        val parsedTests = testParser.extract(componentInfo)
        val tests = applyTestFilters(parsedTests)
        val shard = prepareTestShard(tests, analytics)

        logger.info("Scheduling ${tests.size} tests")
        logger.debug(tests.joinToString(", ") { it.toTestName() })
        scheduler.addTests(shard)
    }

    override suspend fun stopAndWaitForCompletion(): Boolean {
        try {
            scheduler.stopAndWaitForCompletion()
            onFinish(analytics, deviceProvider, attachmentManager)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            // We don't want to catch these. If an exception was thrown, we should fail the execution
            logger.error("Error occurred while finishing tests run", e)
            throw e
        } finally {
            hook.uninstall()
        }
        return progressReporter.aggregateResult()
    }

    private fun installShutdownHook(block: suspend () -> Unit): ShutdownHook {
        val shutdownHook = ShutdownHook(configuration) {
            runBlocking {
                block.invoke()
            }
        }
        shutdownHook.install()
        return shutdownHook
    }

    private suspend fun onFinish(
        analytics: Analytics,
        deviceProvider: DeviceProvider,
        attachmentManager: AttachmentManager
    ) {
        analytics.close()
        deviceProvider.terminate()
        attachmentManager.terminate()
        try {
            tracker.close()
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            throw ReportGenerationException("Failed to generate test run report with exception", e)
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
