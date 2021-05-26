package com.malinskiy.marathon.test.factory

import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.AnalyticsConfiguration
import com.malinskiy.marathon.execution.CacheConfiguration
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.FilteringConfiguration
import com.malinskiy.marathon.execution.MarathonListener
import com.malinskiy.marathon.execution.StrictRunFilterConfiguration
import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.execution.strategy.FlakinessStrategy
import com.malinskiy.marathon.execution.strategy.PoolingStrategy
import com.malinskiy.marathon.execution.strategy.RetryStrategy
import com.malinskiy.marathon.execution.strategy.ShardingStrategy
import com.malinskiy.marathon.execution.strategy.SortingStrategy
import com.malinskiy.marathon.test.Mocks
import com.malinskiy.marathon.test.StubComponentCacheKeyProvider
import com.malinskiy.marathon.test.StubComponentInfoExtractor
import com.malinskiy.marathon.test.StubDeviceProvider
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestVendorConfiguration
import kotlinx.coroutines.channels.Channel
import java.nio.file.Files

fun configuration(block: ConfigurationFactory.() -> Unit = {}) = ConfigurationFactory().apply(block).build()

class ConfigurationFactory {
    var name = "DEFAULT_TEST_CONFIG"
    var outputDir = Files.createTempDirectory("test-run").toFile()
    var vendorConfiguration = TestVendorConfiguration(
        Mocks.TestParser.DEFAULT,
        StubDeviceProvider(),
        StubComponentInfoExtractor(),
        StubComponentCacheKeyProvider()
    )
    var debug: Boolean? = null
    var batchingStrategy: BatchingStrategy? = null
    var customAnalyticsTracker: Tracker? = null
    var analyticsConfiguration: AnalyticsConfiguration? = null
    var excludeSerialRegexes: List<Regex>? = null
    var ignoreFailureRegexes: List<Regex>? = null
    var fallbackToScreenshots: Boolean? = null
    var strictMode: Boolean? = null
    var uncompletedTestRetryQuota: Int? = null
    var filteringConfiguration: FilteringConfiguration? = null
    var pullScreenshotFilterConfiguration: FilteringConfiguration? = null
    var strictRunFilterConfiguration: StrictRunFilterConfiguration? = null
    var listener: MarathonListener? = null
    var flakinessStrategy: FlakinessStrategy? = null
    var cache: CacheConfiguration? = null
    var ignoreFailures: Boolean? = null
    var includeSerialRegexes: List<Regex>? = null
    var isCodeCoverageEnabled: Boolean? = null
    var poolingStrategy: PoolingStrategy? = null
    var retryStrategy: RetryStrategy? = null
    var shardingStrategy: ShardingStrategy? = null
    var sortingStrategy: SortingStrategy? = null
    var testClassRegexes: Collection<Regex>? = null
    var testBatchTimeoutMillis: Long? = null
    var testOutputTimeoutMillis: Long? = null
    var noDevicesTimeoutMillis: Long? = null
    var analyticsTracking: Boolean = false

    fun tests(block: () -> List<Test>) {
        val testParser = vendorConfiguration.testParser()
        (testParser as Mocks.VendorTestParser).tests = block.invoke()
    }

    fun devices(f: suspend (Channel<DeviceProvider.DeviceEvent>) -> Unit) {
        val stubDeviceProvider = vendorConfiguration.deviceProvider()
        stubDeviceProvider.providingLogic = f
    }

    fun build(): Configuration =
        Configuration(
            name = name,
            outputDir = outputDir,
            analyticsConfiguration = analyticsConfiguration,
            customAnalyticsTracker = customAnalyticsTracker,
            poolingStrategy = poolingStrategy,
            shardingStrategy = shardingStrategy,
            sortingStrategy = sortingStrategy,
            batchingStrategy = batchingStrategy,
            flakinessStrategy = flakinessStrategy,
            retryStrategy = retryStrategy,
            filteringConfiguration = filteringConfiguration,
            pullScreenshotFilterConfiguration = pullScreenshotFilterConfiguration,
            strictRunFilterConfiguration = strictRunFilterConfiguration,
            cache = cache,
            ignoreFailures = ignoreFailures,
            isCodeCoverageEnabled = isCodeCoverageEnabled,
            fallbackToScreenshots = fallbackToScreenshots,
            strictMode = strictMode,
            listener = listener,
            uncompletedTestRetryQuota = uncompletedTestRetryQuota,
            testClassRegexes = testClassRegexes,
            includeSerialRegexes = includeSerialRegexes,
            excludeSerialRegexes = excludeSerialRegexes,
            ignoreFailureRegexes = ignoreFailureRegexes,
            testBatchTimeoutMillis = testBatchTimeoutMillis,
            testOutputTimeoutMillis = testOutputTimeoutMillis,
            noDevicesTimeoutMillis = noDevicesTimeoutMillis,
            debug = debug,
            vendorConfiguration = vendorConfiguration,
            analyticsTracking = analyticsTracking
        )
}
