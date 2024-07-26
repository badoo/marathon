package com.malinskiy.marathon

import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.execution.MarathonListener
import org.gradle.api.Action

open class MarathonExtension {
    var customAnalyticsTracker: Tracker? = null

    var poolingStrategy: PoolingStrategyConfiguration? = null
    var shardingStrategy: ShardingStrategyConfiguration? = null
    var sortingStrategy: SortingStrategyConfiguration? = null
    var batchingStrategy: BatchingStrategyConfiguration? = null
    var flakinessStrategy: FlakinessStrategyConfiguration? = null
    var retryStrategy: RetryStrategyConfiguration? = null
    var filteringConfiguration: FilteringPluginConfiguration? = null
    var strictRunFilterConfiguration: StrictRunFilterPluginConfiguration? = null
    var listener: MarathonListener? = null

    var cache: CachePluginConfiguration? = null
    var ignoreFailures: Boolean? = null
    var strictMode: Boolean? = null
    var uncompletedTestRetryQuota: Int? = null

    var testClassRegexes: Collection<String>? = null
    var includeSerialRegexes: Collection<String>? = null
    var excludeSerialRegexes: Collection<String>? = null
    var ignoreFailureRegexes: Collection<String>? = null

    /**
     * Tests that have failed with stack traces that match that property wouldn't be rerun
     * It applies to both failed and uncompleted tests
     * This has higher priority than uncompletedRetriesQuota or amount of runs in StrictRunFilterPluginConfiguration
     */
    var failFastFailureRegexes: Collection<String>? = null

    var testOutputTimeoutMillis: Long? = null
    var noDevicesTimeoutMillis: Long? = null
    var debug: Boolean? = null

    var applicationPmClear: Boolean? = null
    var testApplicationPmClear: Boolean? = null
    var installOptions: String? = null
    var serialStrategy: SerialStrategyConfiguration? = null

    var preferableRecorderType: DeviceFeature? = null

    //Android specific for now
    var autoGrantPermission: Boolean? = null
    var instrumentationArgs: MutableMap<String, String> = mutableMapOf()
    var usedStorageThresholdInPercents: Int? = null

    fun cache(action: Action<CachePluginConfiguration>) {
        cache = (cache ?: CachePluginConfiguration()).also { action.execute(it) }
    }

    fun batchingStrategy(action: Action<BatchingStrategyConfiguration>) {
        batchingStrategy = (batchingStrategy ?: BatchingStrategyConfiguration()).also { action.execute(it) }
    }

    fun flakinessStrategy(action: Action<FlakinessStrategyConfiguration>) {
        flakinessStrategy = (flakinessStrategy ?: FlakinessStrategyConfiguration()).also { action.execute(it) }
    }

    fun poolingStrategy(action: Action<PoolingStrategyConfiguration>) {
        poolingStrategy = (poolingStrategy ?: PoolingStrategyConfiguration()).also { action.execute(it) }
    }

    fun retryStrategy(action: Action<RetryStrategyConfiguration>) {
        retryStrategy = (retryStrategy ?: RetryStrategyConfiguration()).also { action.execute(it) }
    }

    fun shardingStrategy(action: Action<ShardingStrategyConfiguration>) {
        shardingStrategy = (shardingStrategy ?: ShardingStrategyConfiguration()).also { action.execute(it) }
    }

    fun sortingStrategy(action: Action<SortingStrategyConfiguration>) {
        sortingStrategy = (sortingStrategy ?: SortingStrategyConfiguration()).also { action.execute(it) }
    }

    fun filteringConfiguration(action: Action<FilteringPluginConfiguration>) {
        filteringConfiguration = (filteringConfiguration ?: FilteringPluginConfiguration()).also { action.execute(it) }
    }

    fun strictRunFilter(action: Action<StrictRunFilterPluginConfiguration>) {
        strictRunFilterConfiguration = (strictRunFilterConfiguration ?: StrictRunFilterPluginConfiguration()).also { action.execute(it) }
    }

    fun instrumentationArgs(action: Action<MutableMap<String, String>>) {
        action.execute(instrumentationArgs)
    }
}
