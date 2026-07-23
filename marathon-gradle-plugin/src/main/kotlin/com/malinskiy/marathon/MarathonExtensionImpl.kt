package com.malinskiy.marathon

import com.malinskiy.marathon.android.DEFAULT_APPLICATION_PM_CLEAR
import com.malinskiy.marathon.android.DEFAULT_AUTO_GRANT_PERMISSION
import com.malinskiy.marathon.android.DEFAULT_TEST_APPLICATION_PM_CLEAR
import com.malinskiy.marathon.android.DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS

internal abstract class MarathonExtensionImpl : MarathonExtension {
    fun initDefaults() {
        cache.initDefaults()
        poolingStrategy.initDefaults()
        strictRunConfiguration.initDefaults()

        serialStrategy.convention(SerialStrategyConfiguration.AUTOMATIC)
        usedStorageThresholdInPercents.convention(DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS)

        autoGrantPermission.convention(DEFAULT_AUTO_GRANT_PERMISSION)
        debug.convention(true)
        ignoreFailures.convention(false)
        strictMode.convention(false)
        uncompletedTestRetryQuota.convention(3)

        applicationPmClear.convention(DEFAULT_APPLICATION_PM_CLEAR)
        testApplicationPmClear.convention(DEFAULT_TEST_APPLICATION_PM_CLEAR)
    }

    private fun CachePluginConfiguration.initDefaults() {
        local.enabled.convention(true)
        local.removeUnusedEntriesAfterDays.convention(7)
        remote.enabled.convention(true)
        remote.push.convention(true)
    }

    private fun PoolingStrategyConfiguration.initDefaults() {
        operatingSystem.convention(false)
        abi.convention(false)
        manufacturer.convention(false)
        model.convention(false)
    }

    private fun StrictRunPluginConfiguration.initDefaults() {
        runs.convention(1)
    }
}
