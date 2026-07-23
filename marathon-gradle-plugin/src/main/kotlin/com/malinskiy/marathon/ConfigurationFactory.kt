package com.malinskiy.marathon

import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.ddmlib.DdmlibDeviceProviderFactory
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.cache.config.LocalCacheConfiguration
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.execution.AnnotationFilter
import com.malinskiy.marathon.execution.CacheConfiguration
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.FilteringConfiguration
import com.malinskiy.marathon.execution.FullyQualifiedClassnameFilter
import com.malinskiy.marathon.execution.SimpleClassnameFilter
import com.malinskiy.marathon.execution.StrictRunConfiguration
import com.malinskiy.marathon.execution.TestFilter
import com.malinskiy.marathon.execution.TestPackageFilter
import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.execution.strategy.FlakinessStrategy
import com.malinskiy.marathon.execution.strategy.PoolingStrategy
import com.malinskiy.marathon.execution.strategy.RetryStrategy
import com.malinskiy.marathon.execution.strategy.ShardingStrategy
import com.malinskiy.marathon.execution.strategy.SortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.batching.FixedSizeBatchingStrategy
import com.malinskiy.marathon.execution.strategy.impl.batching.IsolateBatchingStrategy
import com.malinskiy.marathon.execution.strategy.impl.flakiness.IgnoreFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.flakiness.ProbabilityBasedFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.OmniPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.AbiPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.ComboPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.ManufacturerPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.ModelPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.OperatingSystemVersionPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.retry.NoRetryStrategy
import com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota.FixedQuotaRetryStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.CountShardingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.ParallelShardingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.ExecutionTimeSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.NoSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.RandomOrderSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.SuccessRateSortingStrategy
import java.io.File
import java.time.Instant

internal fun createCommonConfiguration(
    extensionConfig: MarathonExtension,
    adbPath: File,
    outputDir: File,
    tempDir: File
): Configuration = Configuration(
    outputDir = outputDir,
    tempDir = tempDir,
    cache = extensionConfig.cache.toCacheConfiguration(),
    poolingStrategy = extensionConfig.poolingStrategy.toPoolingStrategy(),
    shardingStrategy = extensionConfig.shardingStrategy.toShardingStrategy(),
    sortingStrategy = extensionConfig.sortingStrategy.toSortingStrategy(),
    batchingStrategy = extensionConfig.batchingStrategy.toBatchingStrategy(),
    flakinessStrategy = extensionConfig.flakinessStrategy.toFlakinessStrategy(),
    retryStrategy = extensionConfig.retryStrategy.toRetryStrategy(),
    filteringConfiguration = extensionConfig.filteringConfiguration.toFilteringConfiguration(),
    strictRunConfiguration = extensionConfig.strictRunConfiguration.toStrictRunConfiguration(),
    ignoreFailures = extensionConfig.ignoreFailures.get(),
    strictMode = extensionConfig.strictMode.get(),
    uncompletedTestRetryQuota = extensionConfig.uncompletedTestRetryQuota.orNull,
    includeSerialRegexes = extensionConfig.includeSerialRegexes.get().map { it.toRegex() },
    excludeSerialRegexes = extensionConfig.excludeSerialRegexes.get().map { it.toRegex() },
    testClassRegexes = extensionConfig.testClassRegexes.get().map { it.toRegex() },
    ignoreFailureRegexes = extensionConfig.ignoreFailureRegexes.get().map { it.toRegex(RegexOption.DOT_MATCHES_ALL) },
    failFastFailureRegexes = extensionConfig.failFastFailureRegexes.get().map { it.toRegex(RegexOption.DOT_MATCHES_ALL) },
    appModuleRegexes = extensionConfig.appModuleRegexes.get().map { it.toRegex(RegexOption.DOT_MATCHES_ALL) },
    testOutputTimeoutMillis = extensionConfig.testOutputTimeoutMillis.orNull,
    noDevicesTimeoutMillis = extensionConfig.noDevicesTimeoutMillis.orNull,
    analyticsTracker = MarathonListenerHolder.analyticsTracker,
    listener = MarathonListenerHolder.listener,
    testOwnerProvider = MarathonListenerHolder.testOwnerProvider,
    vendorConfiguration = createAndroidConfiguration(extensionConfig, adbPath)
)

private fun createAndroidConfiguration(extension: MarathonExtension, adbPath: File): AndroidConfiguration {
    val serialStrategy = when (extension.serialStrategy.get()) {
        SerialStrategyConfiguration.AUTOMATIC -> SerialStrategy.AUTOMATIC
        SerialStrategyConfiguration.MARATHON_PROPERTY -> SerialStrategy.MARATHON_PROPERTY
        SerialStrategyConfiguration.BOOT_PROPERTY -> SerialStrategy.BOOT_PROPERTY
        SerialStrategyConfiguration.HOSTNAME -> SerialStrategy.HOSTNAME
        SerialStrategyConfiguration.DDMS -> SerialStrategy.DDMS
    }
    return AndroidConfiguration(
        adbPath = adbPath,
        deviceProviderFactory = DdmlibDeviceProviderFactory(),
        autoGrantPermission = extension.autoGrantPermission.get(),
        applicationPmClear = extension.applicationPmClear.get(),
        testApplicationPmClear = extension.testApplicationPmClear.get(),
        installOptions = extension.installOptions.get(),
        preferableRecorderType = extension.preferableRecorderType.orNull,
        serialStrategy = serialStrategy,
        usedStorageThresholdInPercents = extension.usedStorageThresholdInPercents.get()
    )
}

private fun CachePluginConfiguration.toCacheConfiguration(): CacheConfiguration =
    CacheConfiguration(
        local = local.toLocalCacheConfiguration(),
        remote = remote.toRemoteCacheConfiguration()
    )

private fun LocalCacheExtension.toLocalCacheConfiguration(): LocalCacheConfiguration =
    if (directory.isPresent && enabled.get()) {
        LocalCacheConfiguration.Enabled(directory.get().asFile, removeUnusedEntriesAfterDays.get())
    } else {
        LocalCacheConfiguration.Disabled
    }

private fun RemoteCacheExtension.toRemoteCacheConfiguration(): RemoteCacheConfiguration =
    if (url.isPresent && enabled.get()) {
        RemoteCacheConfiguration.Enabled(
            url = url.get(),
            push = push.get(),
            accessKey = accessKey.orNull
        )
    } else {
        RemoteCacheConfiguration.Disabled
    }

private fun PoolingStrategyConfiguration.toPoolingStrategy(): PoolingStrategy {
    val strategies = mutableListOf<PoolingStrategy>()
    when {
        operatingSystem.get() -> strategies.add(OperatingSystemVersionPoolingStrategy())
        abi.get() -> strategies.add(AbiPoolingStrategy())
        manufacturer.get() -> strategies.add(ManufacturerPoolingStrategy())
        model.get() -> strategies.add(ModelPoolingStrategy())
    }
    return if (strategies.isNotEmpty()) {
        ComboPoolingStrategy(strategies)
    } else {
        OmniPoolingStrategy()
    }
}

private fun ShardingStrategyConfiguration.toShardingStrategy(): ShardingStrategy =
    if (countSharding.count.isPresent) countSharding.toCountShardingStrategy() else ParallelShardingStrategy()

private fun CountShardingStrategyConfiguration.toCountShardingStrategy(): CountShardingStrategy =
    CountShardingStrategy(count.get())

private fun SortingStrategyConfiguration.toSortingStrategy(): SortingStrategy =
    when {
        executionTime.percentile.isPresent -> executionTime.toExecutionTimeSortingStrategy()
        successRate.limit.isPresent -> successRate.toSuccessRateSortingStrategy()
        randomOrder._initialized.isPresent -> RandomOrderSortingStrategy()
        else -> NoSortingStrategy()
    }

private fun ExecutionTimeSortingStrategyConfiguration.toExecutionTimeSortingStrategy(): ExecutionTimeSortingStrategy =
    ExecutionTimeSortingStrategy(
        percentile = percentile.get(),
        timeLimit = Instant.now().minus(timeLimit.get())
    )

private fun SuccessRateSortingStrategyConfiguration.toSuccessRateSortingStrategy(): SuccessRateSortingStrategy =
    SuccessRateSortingStrategy(
        timeLimit = Instant.now().minus(limit.get()),
        ascending = ascending.get()
    )

private fun BatchingStrategyConfiguration.toBatchingStrategy(): BatchingStrategy =
    if (fixedSize.size.isPresent) fixedSize.toFixedSizeBatchingStrategy() else IsolateBatchingStrategy()

private fun FixedSizeBatchingStrategyConfiguration.toFixedSizeBatchingStrategy(): FixedSizeBatchingStrategy =
    FixedSizeBatchingStrategy(
        size = size.get(),
        durationMillis = durationMillis.orNull,
        percentile = percentile.orNull,
        timeLimit = timeLimit.orNull?.let { Instant.now().minus(it) },
        lastMileLength = lastMileLength.get()
    )

private fun FlakinessStrategyConfiguration.toFlakinessStrategy(): FlakinessStrategy =
    if (probabilityBased.minSuccessRate.isPresent) probabilityBased.toProbabilityBasedFlakinessStrategy() else IgnoreFlakinessStrategy()

private fun ProbabilityBasedFlakinessStrategyConfiguration.toProbabilityBasedFlakinessStrategy(): ProbabilityBasedFlakinessStrategy =
    ProbabilityBasedFlakinessStrategy(
        minSuccessRate = minSuccessRate.get(),
        maxCount = maxCount.get(),
        timeLimit = Instant.now().minus(timeLimit.get())
    )

private fun RetryStrategyConfiguration.toRetryStrategy(): RetryStrategy =
    if (fixedQuota.totalAllowedRetryQuota.isPresent) fixedQuota.toFixedQuotaRetryStrategy() else NoRetryStrategy()

private fun FixedQuotaRetryStrategyConfiguration.toFixedQuotaRetryStrategy(): FixedQuotaRetryStrategy =
    FixedQuotaRetryStrategy(
        totalAllowedRetryQuota = totalAllowedRetryQuota.get(),
        retryPerTestQuota = retryPerTestQuota.get()
    )

private fun FilteringPluginConfiguration.toFilteringConfiguration(): FilteringConfiguration =
    FilteringConfiguration(
        whitelist = whitelist.toList(),
        blacklist = blacklist.toList()
    )

private fun StrictRunPluginConfiguration.toStrictRunConfiguration(): StrictRunConfiguration =
    StrictRunConfiguration(filter.toList(), runs.get())

private fun FilterConfiguration.toList(): List<TestFilter> =
    annotationFilter.get().map { AnnotationFilter(it.toRegex()) } +
        fullyQualifiedClassnameFilter.get().map { FullyQualifiedClassnameFilter(it.toRegex()) } +
        testPackageFilter.get().map { TestPackageFilter(it.toRegex()) } +
        simpleClassNameFilter.get().map { SimpleClassnameFilter(it.toRegex()) }
