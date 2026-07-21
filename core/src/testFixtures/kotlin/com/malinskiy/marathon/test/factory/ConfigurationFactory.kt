package com.malinskiy.marathon.test.factory

import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.device.DeviceEvent
import com.malinskiy.marathon.execution.CacheConfiguration
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.FilteringConfiguration
import com.malinskiy.marathon.execution.MarathonListener
import com.malinskiy.marathon.execution.StrictRunConfiguration
import com.malinskiy.marathon.execution.TestOwnerProvider
import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.execution.strategy.FlakinessStrategy
import com.malinskiy.marathon.execution.strategy.PoolingStrategy
import com.malinskiy.marathon.execution.strategy.RetryStrategy
import com.malinskiy.marathon.execution.strategy.ShardingStrategy
import com.malinskiy.marathon.execution.strategy.SortingStrategy
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.vendor.StubVendorConfiguration
import com.malinskiy.marathon.vendor.VendorConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.nio.file.Files

fun configuration(block: ConfigurationFactory.() -> Unit = {}) = ConfigurationFactory().apply(block).build()

class ConfigurationFactory {
    var outputDir: File = Files.createTempDirectory("test-run").toFile()
    var cache: CacheConfiguration? = null
    var poolingStrategy: PoolingStrategy? = null
    var shardingStrategy: ShardingStrategy? = null
    var sortingStrategy: SortingStrategy? = null
    var batchingStrategy: BatchingStrategy? = null
    var flakinessStrategy: FlakinessStrategy? = null
    var retryStrategy: RetryStrategy? = null
    var filteringConfiguration: FilteringConfiguration? = null
    var strictRunConfiguration: StrictRunConfiguration? = null
    var ignoreFailures: Boolean? = null
    var strictMode: Boolean? = null
    var uncompletedTestRetryQuota: Int? = null
    var includeSerialRegexes: List<Regex>? = null
    var excludeSerialRegexes: List<Regex>? = null
    var testClassRegexes: Collection<Regex>? = null
    var ignoreFailureRegexes: List<Regex>? = null
    var failFastFailureRegexes: List<Regex>? = null
    var appModuleRegexes: List<Regex>? = null
    var testOutputTimeoutMillis: Long? = null
    var noDevicesTimeoutMillis: Long? = null
    var analyticsTracker: Tracker? = null
    var testOwnerProvider: TestOwnerProvider? = null
    var listener: MarathonListener? = null
    var vendorConfiguration: VendorConfiguration = StubVendorConfiguration()

    private val stubVendorConfiguration: StubVendorConfiguration
        get() = vendorConfiguration as? StubVendorConfiguration
            ?: error("This helper requires the default StubVendorConfiguration")

    fun tests(block: () -> List<Test>) {
        val testParser = stubVendorConfiguration.testParser
        testParser.tests = block.invoke()
    }

    fun devices(f: suspend (Channel<DeviceEvent>) -> Unit) {
        val stubDeviceProvider = stubVendorConfiguration.deviceProvider
        stubDeviceProvider.providingLogic = f
    }

    fun deviceProviderScope(scope: CoroutineScope) {
        stubVendorConfiguration.deviceProvider.coroutineScope = scope
    }

    fun build(): Configuration =
        Configuration(
            outputDir = outputDir,
            tempDir = File(outputDir, "tmp"),
            cache = cache,
            poolingStrategy = poolingStrategy,
            shardingStrategy = shardingStrategy,
            sortingStrategy = sortingStrategy,
            batchingStrategy = batchingStrategy,
            flakinessStrategy = flakinessStrategy,
            retryStrategy = retryStrategy,
            filteringConfiguration = filteringConfiguration,
            strictRunConfiguration = strictRunConfiguration,
            ignoreFailures = ignoreFailures,
            strictMode = strictMode,
            uncompletedTestRetryQuota = uncompletedTestRetryQuota,
            includeSerialRegexes = includeSerialRegexes,
            excludeSerialRegexes = excludeSerialRegexes,
            testClassRegexes = testClassRegexes,
            ignoreFailureRegexes = ignoreFailureRegexes,
            failFastFailureRegexes = failFastFailureRegexes,
            appModuleRegexes = appModuleRegexes,
            testOutputTimeoutMillis = testOutputTimeoutMillis,
            noDevicesTimeoutMillis = noDevicesTimeoutMillis,
            analyticsTracker = analyticsTracker,
            listener = listener,
            testOwnerProvider = testOwnerProvider,
            vendorConfiguration = vendorConfiguration
        )
}
