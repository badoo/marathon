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

data class Configuration constructor(
    val outputDir: File,

    val customAnalyticsTracker: Tracker?,
    val poolingStrategy: PoolingStrategy,
    val shardingStrategy: ShardingStrategy,
    val sortingStrategy: SortingStrategy,
    val batchingStrategy: BatchingStrategy,
    val flakinessStrategy: FlakinessStrategy,
    val retryStrategy: RetryStrategy,
    val filteringConfiguration: FilteringConfiguration,
    val strictRunFilterConfiguration: StrictRunFilterConfiguration,
    val listener: MarathonListener?,

    val cache: CacheConfiguration,
    val ignoreFailures: Boolean,
    val strictMode: Boolean,
    val uncompletedTestRetryQuota: Int,

    val testClassRegexes: Collection<Regex>,
    val includeSerialRegexes: Collection<Regex>,
    val excludeSerialRegexes: Collection<Regex>,
    val ignoreFailureRegexes: Collection<Regex>,
    val failFastFailureRegexes: Collection<Regex>,

    val testOutputTimeoutMillis: Long,
    val noDevicesTimeoutMillis: Long,
    val debug: Boolean,

    val vendorConfiguration: VendorConfiguration
) {

    constructor(
        outputDir: File,

        customAnalyticsTracker: Tracker?,
        poolingStrategy: PoolingStrategy?,
        shardingStrategy: ShardingStrategy?,
        sortingStrategy: SortingStrategy?,
        batchingStrategy: BatchingStrategy?,
        flakinessStrategy: FlakinessStrategy?,
        retryStrategy: RetryStrategy?,
        filteringConfiguration: FilteringConfiguration?,
        strictRunFilterConfiguration: StrictRunFilterConfiguration?,
        listener: MarathonListener?,

        cache: CacheConfiguration?,
        ignoreFailures: Boolean?,
        strictMode: Boolean?,
        uncompletedTestRetryQuota: Int?,

        testClassRegexes: Collection<Regex>?,
        includeSerialRegexes: Collection<Regex>?,
        excludeSerialRegexes: Collection<Regex>?,
        ignoreFailureRegexes: Collection<Regex>?,
        failFastFailureRegexes: Collection<Regex>?,

        testOutputTimeoutMillis: Long?,
        noDevicesTimeoutMillis: Long?,
        debug: Boolean?,

        vendorConfiguration: VendorConfiguration
    ) :

        this(
            outputDir = outputDir,
            customAnalyticsTracker = customAnalyticsTracker,
            poolingStrategy = poolingStrategy ?: OmniPoolingStrategy(),
            shardingStrategy = shardingStrategy ?: ParallelShardingStrategy(),
            sortingStrategy = sortingStrategy ?: NoSortingStrategy(),
            batchingStrategy = batchingStrategy ?: IsolateBatchingStrategy(),
            flakinessStrategy = flakinessStrategy ?: IgnoreFlakinessStrategy(),
            retryStrategy = retryStrategy ?: NoRetryStrategy(),
            filteringConfiguration = filteringConfiguration ?: FilteringConfiguration(emptyList(), emptyList()),
            strictRunFilterConfiguration = strictRunFilterConfiguration ?: StrictRunFilterConfiguration(emptyList()),
            cache = cache ?: CacheConfiguration(),
            ignoreFailures = ignoreFailures ?: false,
            strictMode = strictMode ?: false,
            listener = listener,
            uncompletedTestRetryQuota = uncompletedTestRetryQuota ?: Integer.MAX_VALUE,
            testClassRegexes = testClassRegexes ?: listOf(Regex("^((?!Abstract).)*Test$")),
            includeSerialRegexes = includeSerialRegexes ?: emptyList(),
            excludeSerialRegexes = excludeSerialRegexes ?: emptyList(),
            ignoreFailureRegexes = ignoreFailureRegexes ?: emptyList(),
            failFastFailureRegexes = failFastFailureRegexes ?: emptyList(),
            testOutputTimeoutMillis = testOutputTimeoutMillis ?: DEFAULT_OUTPUT_TIMEOUT_MILLIS,
            noDevicesTimeoutMillis = noDevicesTimeoutMillis ?: DEFAULT_NO_DEVICES_TIMEOUT_MILLIS,
            debug = debug ?: true,
            vendorConfiguration = vendorConfiguration
        )

    fun toMap() =
        mapOf<String, String>(
            "outputDir" to outputDir.absolutePath,
            "pooling" to poolingStrategy.toString(),
            "sharding" to shardingStrategy.toString(),
            "sorting" to sortingStrategy.toString(),
            "batching" to batchingStrategy.toString(),
            "flakiness" to flakinessStrategy.toString(),
            "retry" to retryStrategy.toString(),
            "filtering" to filteringConfiguration.toString(),
            "strictRunFilter" to strictRunFilterConfiguration.toString(),
            "cache" to cache.toString(),
            "ignoreFailures" to ignoreFailures.toString(),
            "strictMode" to strictMode.toString(),
            "testClassRegexes" to testClassRegexes.toString(),
            "includeSerialRegexes" to includeSerialRegexes.toString(),
            "excludeSerialRegexes" to excludeSerialRegexes.toString(),
            "testOutputTimeoutMillis" to testOutputTimeoutMillis.toString(),
            "noDevicesTimeoutMillis" to noDevicesTimeoutMillis.toString(),
            "debug" to debug.toString(),
            "vendorConfiguration" to vendorConfiguration.toString()
        )
}
