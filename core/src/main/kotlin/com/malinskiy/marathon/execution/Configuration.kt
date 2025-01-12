package com.malinskiy.marathon.execution

import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.execution.strategy.FlakinessStrategy
import com.malinskiy.marathon.execution.strategy.PoolingStrategy
import com.malinskiy.marathon.execution.strategy.RetryStrategy
import com.malinskiy.marathon.execution.strategy.ShardingStrategy
import com.malinskiy.marathon.execution.strategy.SortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.batching.IsolateBatchingStrategy
import com.malinskiy.marathon.execution.strategy.impl.flakiness.IgnoreFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.OmniPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.retry.NoRetryStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.ParallelShardingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.NoSortingStrategy
import com.malinskiy.marathon.vendor.VendorConfiguration
import java.io.File

private const val DEFAULT_NO_DEVICES_TIMEOUT_MILLIS: Long = 300_000
private const val DEFAULT_OUTPUT_TIMEOUT_MILLIS: Long = 60_000

data class Configuration(
    val outputDir: File,

    val cache: CacheConfiguration,
    val poolingStrategy: PoolingStrategy,
    val shardingStrategy: ShardingStrategy,
    val sortingStrategy: SortingStrategy,
    val batchingStrategy: BatchingStrategy,
    val flakinessStrategy: FlakinessStrategy,
    val retryStrategy: RetryStrategy,
    val filteringConfiguration: FilteringConfiguration,
    val strictRunConfiguration: StrictRunConfiguration,

    val ignoreFailures: Boolean,
    val strictMode: Boolean,
    val uncompletedTestRetryQuota: Int,

    val includeSerialRegexes: Collection<Regex>,
    val excludeSerialRegexes: Collection<Regex>,
    val testClassRegexes: Collection<Regex>,
    val ignoreFailureRegexes: Collection<Regex>,
    val failFastFailureRegexes: Collection<Regex>,
    val appModuleRegexes: Collection<Regex>,

    val testOutputTimeoutMillis: Long,
    val noDevicesTimeoutMillis: Long,

    val analyticsTracker: Tracker?,
    val listener: MarathonListener?,
    val vendorConfiguration: VendorConfiguration
) {

    constructor(
        outputDir: File,

        cache: CacheConfiguration?,
        poolingStrategy: PoolingStrategy?,
        shardingStrategy: ShardingStrategy?,
        sortingStrategy: SortingStrategy?,
        batchingStrategy: BatchingStrategy?,
        flakinessStrategy: FlakinessStrategy?,
        retryStrategy: RetryStrategy?,
        filteringConfiguration: FilteringConfiguration?,
        strictRunConfiguration: StrictRunConfiguration?,

        ignoreFailures: Boolean?,
        strictMode: Boolean?,
        uncompletedTestRetryQuota: Int?,

        includeSerialRegexes: Collection<Regex>?,
        excludeSerialRegexes: Collection<Regex>?,
        testClassRegexes: Collection<Regex>?,
        ignoreFailureRegexes: Collection<Regex>?,
        failFastFailureRegexes: Collection<Regex>?,
        appModuleRegexes: Collection<Regex>?,

        testOutputTimeoutMillis: Long?,
        noDevicesTimeoutMillis: Long?,

        analyticsTracker: Tracker?,
        listener: MarathonListener?,
        vendorConfiguration: VendorConfiguration
    ) :

        this(
            outputDir = outputDir,
            cache = cache ?: CacheConfiguration(),
            poolingStrategy = poolingStrategy ?: OmniPoolingStrategy(),
            shardingStrategy = shardingStrategy ?: ParallelShardingStrategy(),
            sortingStrategy = sortingStrategy ?: NoSortingStrategy(),
            batchingStrategy = batchingStrategy ?: IsolateBatchingStrategy(),
            flakinessStrategy = flakinessStrategy ?: IgnoreFlakinessStrategy(),
            retryStrategy = retryStrategy ?: NoRetryStrategy(),
            filteringConfiguration = filteringConfiguration ?: FilteringConfiguration(),
            strictRunConfiguration = strictRunConfiguration ?: StrictRunConfiguration(),
            ignoreFailures = ignoreFailures ?: false,
            strictMode = strictMode ?: false,
            uncompletedTestRetryQuota = uncompletedTestRetryQuota ?: Integer.MAX_VALUE,
            includeSerialRegexes = includeSerialRegexes ?: emptyList(),
            excludeSerialRegexes = excludeSerialRegexes ?: emptyList(),
            testClassRegexes = testClassRegexes ?: listOf(Regex("^((?!Abstract).)*Test$")),
            ignoreFailureRegexes = ignoreFailureRegexes ?: emptyList(),
            failFastFailureRegexes = failFastFailureRegexes ?: emptyList(),
            appModuleRegexes = appModuleRegexes ?: emptyList(),
            testOutputTimeoutMillis = testOutputTimeoutMillis ?: DEFAULT_OUTPUT_TIMEOUT_MILLIS,
            noDevicesTimeoutMillis = noDevicesTimeoutMillis ?: DEFAULT_NO_DEVICES_TIMEOUT_MILLIS,
            analyticsTracker = analyticsTracker,
            listener = listener,
            vendorConfiguration = vendorConfiguration
        )

    fun toMap() =
        mapOf<String, String>(
            "outputDir" to outputDir.absolutePath,
            "cache" to cache.toString(),
            "pooling" to poolingStrategy.toString(),
            "sharding" to shardingStrategy.toString(),
            "sorting" to sortingStrategy.toString(),
            "batching" to batchingStrategy.toString(),
            "flakiness" to flakinessStrategy.toString(),
            "retry" to retryStrategy.toString(),
            "filtering" to filteringConfiguration.toString(),
            "strictRun" to strictRunConfiguration.toString(),
            "ignoreFailures" to ignoreFailures.toString(),
            "strictMode" to strictMode.toString(),
            "includeSerialRegexes" to includeSerialRegexes.toString(),
            "excludeSerialRegexes" to excludeSerialRegexes.toString(),
            "testClassRegexes" to testClassRegexes.toString(),
            "testOutputTimeoutMillis" to testOutputTimeoutMillis.toString(),
            "noDevicesTimeoutMillis" to noDevicesTimeoutMillis.toString(),
            "vendorConfiguration" to vendorConfiguration.toString()
        )
}
