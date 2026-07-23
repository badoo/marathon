package com.malinskiy.marathon

import com.malinskiy.marathon.device.DeviceFeature
import org.gradle.api.Action
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

interface MarathonExtension {
    @get:Nested
    val cache: CachePluginConfiguration

    @get:Nested
    val poolingStrategy: PoolingStrategyConfiguration

    @get:Nested
    val shardingStrategy: ShardingStrategyConfiguration

    @get:Nested
    val sortingStrategy: SortingStrategyConfiguration

    @get:Nested
    val batchingStrategy: BatchingStrategyConfiguration

    @get:Nested
    val flakinessStrategy: FlakinessStrategyConfiguration

    @get:Nested
    val retryStrategy: RetryStrategyConfiguration

    @get:Nested
    val filteringConfiguration: FilteringPluginConfiguration

    @get:Nested
    val strictRunConfiguration: StrictRunPluginConfiguration

    val serialStrategy: Property<SerialStrategyConfiguration>
    val preferableRecorderType: Property<DeviceFeature>

    val autoGrantPermission: Property<Boolean>
    val debug: Property<Boolean>
    val ignoreFailures: Property<Boolean>
    val strictMode: Property<Boolean>

    val applicationPmClear: Property<Boolean>
    val testApplicationPmClear: Property<Boolean>

    val includeSerialRegexes: ListProperty<String>
    val excludeSerialRegexes: ListProperty<String>
    val testClassRegexes: ListProperty<String>
    val ignoreFailureRegexes: ListProperty<String>

    /**
     * Tests that have failed with stack traces that match that property wouldn't be rerun
     * It applies to both failed and uncompleted tests
     * This has higher priority than uncompletedRetriesQuota or amount of runs in StrictRunFilterPluginConfiguration
     */
    val failFastFailureRegexes: ListProperty<String>

    /**
     * Application modules, needed for distinguish between application and component tests in Allure
     */
    val appModuleRegexes: ListProperty<String>

    val uncompletedTestRetryQuota: Property<Int>
    val usedStorageThresholdInPercents: Property<Int>
    val testOutputTimeoutMillis: Property<Long>
    val noDevicesTimeoutMillis: Property<Long>

    val installOptions: ListProperty<String>

    fun cache(action: Action<CachePluginConfiguration>) {
        action.execute(cache)
    }

    fun batchingStrategy(action: Action<BatchingStrategyConfiguration>) {
        action.execute(batchingStrategy)
    }

    fun flakinessStrategy(action: Action<FlakinessStrategyConfiguration>) {
        action.execute(flakinessStrategy)
    }

    fun poolingStrategy(action: Action<PoolingStrategyConfiguration>) {
        action.execute(poolingStrategy)
    }

    fun retryStrategy(action: Action<RetryStrategyConfiguration>) {
        action.execute(retryStrategy)
    }

    fun shardingStrategy(action: Action<ShardingStrategyConfiguration>) {
        action.execute(shardingStrategy)
    }

    fun sortingStrategy(action: Action<SortingStrategyConfiguration>) {
        action.execute(sortingStrategy)
    }

    fun filteringConfiguration(action: Action<FilteringPluginConfiguration>) {
        action.execute(filteringConfiguration)
    }

    fun strictRunConfiguration(action: Action<StrictRunPluginConfiguration>) {
        action.execute(strictRunConfiguration)
    }

    companion object {
        const val NAME = "marathon"
    }
}
