package com.malinskiy.marathon

import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.device.DeviceFeature
import groovy.lang.Closure
import org.gradle.api.Project

open class MarathonExtension(project: Project) {
    var customAnalyticsTracker: Tracker? = null
    var analyticsConfiguration: AnalyticsConfig? = null

    var poolingStrategy: PoolingStrategyConfiguration? = null
    var shardingStrategy: ShardingStrategyConfiguration? = null
    var sortingStrategy: SortingStrategyConfiguration? = null
    var batchingStrategy: BatchingStrategyConfiguration? = null
    var flakinessStrategy: FlakinessStrategyConfiguration? = null
    var retryStrategy: RetryStrategyConfiguration? = null
    var filteringConfiguration: FilteringPluginConfiguration? = null
    var strictRunFilterConfiguration: StrictRunFilterPluginConfiguration? = null

    var baseOutputDir: String? = null

    var cache: CachePluginConfiguration? = null
    var ignoreFailures: Boolean? = null
    var isCodeCoverageEnabled: Boolean? = null
    var fallbackToScreenshots: Boolean? = null
    var strictMode: Boolean? = null
    var uncompletedTestRetryQuota: Int? = null

    var testClassRegexes: Collection<String>? = null
    var includeSerialRegexes: Collection<String>? = null
    var excludeSerialRegexes: Collection<String>? = null

    var testBatchTimeoutMillis: Long? = null
    var testOutputTimeoutMillis: Long? = null
    var debug: Boolean? = null

    var applicationPmClear: Boolean? = null
    var testApplicationPmClear: Boolean? = null
    var adbInitTimeout: Int? = null
    var installOptions: String? = null
    var serialStrategy: SerialStrategyConfiguration? = null

    var preferableRecorderType: DeviceFeature? = null

    var analyticsTracking: Boolean = false

    //Android specific for now
    var autoGrantPermission: Boolean? = null
    var instrumentationArgs: MutableMap<String, String> = mutableMapOf()
    var pullScreenshotFilterConfiguration: FilteringPluginConfiguration? = null

    //Kotlin way
    fun cache(block: CachePluginConfiguration.() -> Unit) {
        cache = CachePluginConfiguration().also(block)
    }

    fun analytics(block: AnalyticsConfig.() -> Unit) {
        analyticsConfiguration = AnalyticsConfig().also(block)
    }

    fun batchingStrategy(block: BatchingStrategyConfiguration.() -> Unit) {
        batchingStrategy = BatchingStrategyConfiguration().also(block)
    }

    fun flakinessStrategy(block: FlakinessStrategyConfiguration.() -> Unit) {
        flakinessStrategy = FlakinessStrategyConfiguration().also(block)
    }

    fun poolingStrategy(block: PoolingStrategyConfiguration.() -> Unit) {
        poolingStrategy = PoolingStrategyConfiguration().also(block)
    }

    fun retryStrategy(block: RetryStrategyConfiguration.() -> Unit) {
        retryStrategy = RetryStrategyConfiguration().also(block)
    }

    fun shardingStrategy(block: ShardingStrategyConfiguration.() -> Unit) {
        shardingStrategy = ShardingStrategyConfiguration().also(block)
    }

    fun sortingStrategy(block: SortingStrategyConfiguration.() -> Unit) {
        sortingStrategy = SortingStrategyConfiguration().also(block)
    }

    fun filteringConfiguration(block: FilteringPluginConfiguration.() -> Unit) {
        filteringConfiguration = FilteringPluginConfiguration().also(block)
    }

    fun strictRunFilter(block: StrictRunFilterPluginConfiguration.() -> Unit) {
        strictRunFilterConfiguration = StrictRunFilterPluginConfiguration().also(block)
    }

    fun instrumentationArgs(block: MutableMap<String, String>.() -> Unit) {
        instrumentationArgs = mutableMapOf<String, String>().also(block)
    }

    fun pullScreenshotFilterConfiguration(block: FilteringPluginConfiguration.() -> Unit) {
        pullScreenshotFilterConfiguration = FilteringPluginConfiguration().also(block)
    }

    //Groovy way
    fun cache(closure: Closure<*>) {
        cache = CachePluginConfiguration()
        closure.delegate = cache
        closure.call()
    }

    fun analytics(closure: Closure<*>) {
        analyticsConfiguration = AnalyticsConfig()
        closure.delegate = analyticsConfiguration
        closure.call()
    }

    fun batchingStrategy(closure: Closure<*>) {
        batchingStrategy = BatchingStrategyConfiguration()
        closure.delegate = batchingStrategy
        closure.call()
    }

    fun flakinessStrategy(closure: Closure<*>) {
        flakinessStrategy = FlakinessStrategyConfiguration()
        closure.delegate = flakinessStrategy
        closure.call()
    }

    fun poolingStrategy(closure: Closure<*>) {
        poolingStrategy = PoolingStrategyConfiguration()
        closure.delegate = poolingStrategy
        closure.call()
    }

    fun retryStrategy(closure: Closure<*>) {
        retryStrategy = RetryStrategyConfiguration()
        closure.delegate = retryStrategy
        closure.call()
    }

    fun shardingStrategy(closure: Closure<*>) {
        shardingStrategy = ShardingStrategyConfiguration()
        closure.delegate = shardingStrategy
        closure.call()
    }

    fun sortingStrategy(closure: Closure<*>) {
        sortingStrategy = SortingStrategyConfiguration()
        closure.delegate = sortingStrategy
        closure.call()
    }

    fun filteringConfiguration(closure: Closure<*>) {
        filteringConfiguration = FilteringPluginConfiguration()
        closure.delegate = filteringConfiguration
        closure.call()
    }

    fun pullScreenshotFilterConfiguration(closure: Closure<*>) {
        pullScreenshotFilterConfiguration = FilteringPluginConfiguration()
        closure.delegate = pullScreenshotFilterConfiguration
        closure.call()
    }

    fun strictRunFilter(closure: Closure<*>) {
        strictRunFilterConfiguration = StrictRunFilterPluginConfiguration()
        closure.delegate = strictRunFilterConfiguration
        closure.call()
    }

    fun instrumentationArgs(closure: Closure<*>) {
        instrumentationArgs = mutableMapOf()
        closure.delegate = instrumentationArgs
        closure.call()
    }
}
