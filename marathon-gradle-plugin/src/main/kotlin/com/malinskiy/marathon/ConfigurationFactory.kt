package com.malinskiy.marathon

import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.execution.Configuration
import ddmlibModule
import java.io.File

internal fun createCommonConfiguration(
    extensionConfig: MarathonExtension,
    adbPath: File,
    outputDir: File
): Configuration = Configuration(
    outputDir = outputDir,
    cache = extensionConfig.cache.toCacheConfiguration(),
    poolingStrategy = extensionConfig.poolingStrategy.toStrategy(),
    shardingStrategy = extensionConfig.shardingStrategy.toStrategy(),
    sortingStrategy = extensionConfig.sortingStrategy.toStrategy(),
    batchingStrategy = extensionConfig.batchingStrategy.toStrategy(),
    flakinessStrategy = extensionConfig.flakinessStrategy.toStrategy(),
    retryStrategy = extensionConfig.retryStrategy.toStrategy(),
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
    testOutputTimeoutMillis = extensionConfig.testOutputTimeoutMillis.orNull,
    noDevicesTimeoutMillis = extensionConfig.noDevicesTimeoutMillis.orNull,
    analyticsTracker = MarathonListenerHolder.analyticsTracker,
    listener = MarathonListenerHolder.listener,
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
        implementationModules = listOf(ddmlibModule),
        autoGrantPermission = extension.autoGrantPermission.get(),
        instrumentationArgs = extension.instrumentationArgs.get(),
        applicationPmClear = extension.applicationPmClear.get(),
        testApplicationPmClear = extension.testApplicationPmClear.get(),
        installOptions = extension.installOptions.get(),
        preferableRecorderType = extension.preferableRecorderType.orNull,
        serialStrategy = serialStrategy,
        usedStorageThresholdInPercents = extension.usedStorageThresholdInPercents.get()
    )
}
