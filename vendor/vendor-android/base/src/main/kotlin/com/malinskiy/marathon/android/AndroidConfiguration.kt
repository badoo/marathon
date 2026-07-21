package com.malinskiy.marathon.android

import com.malinskiy.marathon.android.executor.logcat.LogcatCollector
import com.malinskiy.marathon.android.executor.logcat.parse.LogcatEventsAdapter
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.vendor.VendorComponents
import com.malinskiy.marathon.vendor.VendorConfiguration
import com.malinskiy.marathon.vendor.VendorDependencies
import java.io.File

const val DEFAULT_AUTO_GRANT_PERMISSION = false
const val DEFAULT_APPLICATION_PM_CLEAR = false
const val DEFAULT_TEST_APPLICATION_PM_CLEAR = false
const val DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS = 85

class AndroidConfiguration(
    val adbPath: File,
    val deviceProviderFactory: DeviceProviderFactory,
    val autoGrantPermission: Boolean = DEFAULT_AUTO_GRANT_PERMISSION,
    val applicationPmClear: Boolean = DEFAULT_APPLICATION_PM_CLEAR,
    val testApplicationPmClear: Boolean = DEFAULT_TEST_APPLICATION_PM_CLEAR,
    val installOptions: List<String> = emptyList(),
    val preferableRecorderType: DeviceFeature? = null,
    val serialStrategy: SerialStrategy = SerialStrategy.AUTOMATIC,
    val usedStorageThresholdInPercents: Int = DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS
) : VendorConfiguration {

    override fun preferableRecorderType(): DeviceFeature? = preferableRecorderType

    override fun createComponents(dependencies: VendorDependencies): VendorComponents {
        val logcatCollector = LogcatCollector(dependencies.tempFileFactory)
        return VendorComponents(
            deviceProvider = deviceProviderFactory.create(dependencies, LogcatEventsAdapter(logcatCollector)),
            testParser = AndroidTestParser(),
            logsProvider = logcatCollector,
            componentCacheKeyProvider = AndroidComponentCacheKeyProvider(dependencies.fileHasher)
        )
    }

    override fun toString(): String =
        "AndroidConfiguration(adbPath=$adbPath, autoGrantPermission=$autoGrantPermission, " +
            "applicationPmClear=$applicationPmClear, testApplicationPmClear=$testApplicationPmClear, installOptions=$installOptions, " +
            "preferableRecorderType=$preferableRecorderType, serialStrategy=$serialStrategy, " +
            "usedStorageThresholdInPercents=$usedStorageThresholdInPercents)"
}
