package com.malinskiy.marathon

import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.DEFAULT_APPLICATION_PM_CLEAR
import com.malinskiy.marathon.android.DEFAULT_AUTO_GRANT_PERMISSION
import com.malinskiy.marathon.android.DEFAULT_INSTALL_OPTIONS
import com.malinskiy.marathon.android.DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS
import com.malinskiy.marathon.android.defaultInitTimeoutMillis
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.execution.Configuration
import ddmlibModule
import org.gradle.api.Project
import java.io.File

internal fun createCommonConfiguration(
    project: Project,
    marathonConfig: MarathonExtension,
    sdkDirectory: File
): Configuration {
    val output = getOutputDirectory(project, marathonConfig)
    val fakeApk = File(".")
    val fakeName = "marathon-common"

    return createConfiguration(
        extensionConfig = marathonConfig,
        applicationApk = null,
        instrumentationApk = fakeApk,
        sdkDirectory = sdkDirectory,
        name = fakeName,
        output = output
    )
}

private fun createConfiguration(
    extensionConfig: MarathonExtension,
    applicationApk: File?,
    instrumentationApk: File,
    sdkDirectory: File,
    name: String,
    output: File
): Configuration = Configuration(
    name = name,
    outputDir = output,
    customAnalyticsTracker = extensionConfig.customAnalyticsTracker,
    poolingStrategy = extensionConfig.poolingStrategy?.toStrategy(),
    shardingStrategy = extensionConfig.shardingStrategy?.toStrategy(),
    sortingStrategy = extensionConfig.sortingStrategy?.toStrategy(),
    batchingStrategy = extensionConfig.batchingStrategy?.toStrategy(),
    flakinessStrategy = extensionConfig.flakinessStrategy?.toStrategy(),
    retryStrategy = extensionConfig.retryStrategy?.toStrategy(),
    filteringConfiguration = extensionConfig.filteringConfiguration?.toFilteringConfiguration(),
    strictRunFilterConfiguration = extensionConfig.strictRunFilterConfiguration?.toStrictRunFilterConfiguration(),
    cache = extensionConfig.cache?.toCacheConfiguration(),
    ignoreFailures = extensionConfig.ignoreFailures,
    isCodeCoverageEnabled = extensionConfig.isCodeCoverageEnabled,
    fallbackToScreenshots = extensionConfig.fallbackToScreenshots,
    strictMode = extensionConfig.strictMode,
    listener = extensionConfig.listener,
    uncompletedTestRetryQuota = extensionConfig.uncompletedTestRetryQuota,
    testClassRegexes = extensionConfig.testClassRegexes?.map { it.toRegex() },
    includeSerialRegexes = extensionConfig.includeSerialRegexes?.map { it.toRegex() },
    excludeSerialRegexes = extensionConfig.excludeSerialRegexes?.map { it.toRegex() },
    ignoreFailureRegexes = extensionConfig.ignoreFailureRegexes?.map { it.toRegex(RegexOption.DOT_MATCHES_ALL) },
    failFastFailureRegexes = extensionConfig.failFastFailureRegexes?.map { it.toRegex(RegexOption.DOT_MATCHES_ALL) },
    testBatchTimeoutMillis = extensionConfig.testBatchTimeoutMillis,
    testOutputTimeoutMillis = extensionConfig.testOutputTimeoutMillis,
    noDevicesTimeoutMillis = extensionConfig.noDevicesTimeoutMillis,
    debug = extensionConfig.debug,
    vendorConfiguration = createAndroidConfiguration(extensionConfig, applicationApk, instrumentationApk, sdkDirectory)
)

private fun getOutputDirectory(project: Project, extensionConfig: MarathonExtension): File =
    extensionConfig.baseOutputDir?.let { File(it) }
        ?: project.layout.buildDirectory.dir("reports/marathon").get().asFile

private fun createAndroidConfiguration(
    extension: MarathonExtension,
    applicationApk: File?,
    instrumentationApk: File,
    sdkDirectory: File
): AndroidConfiguration {
    val autoGrantPermission = extension.autoGrantPermission ?: DEFAULT_AUTO_GRANT_PERMISSION
    val instrumentationArgs = extension.instrumentationArgs
    val applicationPmClear = extension.applicationPmClear ?: DEFAULT_APPLICATION_PM_CLEAR
    val testApplicationPmClear = extension.testApplicationPmClear ?: DEFAULT_APPLICATION_PM_CLEAR
    val adbInitTimeout = extension.adbInitTimeout ?: defaultInitTimeoutMillis
    val installOptions = extension.installOptions ?: DEFAULT_INSTALL_OPTIONS
    val preferableRecorderType = extension.preferableRecorderType
    val serialStrategy = extension.serialStrategy
        ?.let {
            when (it) {
                SerialStrategyConfiguration.AUTOMATIC -> SerialStrategy.AUTOMATIC
                SerialStrategyConfiguration.MARATHON_PROPERTY -> SerialStrategy.MARATHON_PROPERTY
                SerialStrategyConfiguration.BOOT_PROPERTY -> SerialStrategy.BOOT_PROPERTY
                SerialStrategyConfiguration.HOSTNAME -> SerialStrategy.HOSTNAME
                SerialStrategyConfiguration.DDMS -> SerialStrategy.DDMS
            }
        }
        ?: SerialStrategy.AUTOMATIC
    val usedStorageThresholdInPercents = extension.usedStorageThresholdInPercents ?: DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS

    return AndroidConfiguration(
        sdkDirectory,
        applicationApk,
        instrumentationApk,
        listOf(ddmlibModule),
        autoGrantPermission,
        instrumentationArgs,
        applicationPmClear,
        testApplicationPmClear,
        adbInitTimeout,
        installOptions,
        preferableRecorderType,
        serialStrategy,
        usedStorageThresholdInPercents
    )
}
