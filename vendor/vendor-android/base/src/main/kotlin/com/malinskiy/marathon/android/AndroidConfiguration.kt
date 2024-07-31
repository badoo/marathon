package com.malinskiy.marathon.android

import com.malinskiy.marathon.android.di.androidModule
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.vendor.VendorConfiguration
import org.koin.core.module.Module
import java.io.File

const val DEFAULT_AUTO_GRANT_PERMISSION = false
const val DEFAULT_APPLICATION_PM_CLEAR = false
const val DEFAULT_TEST_APPLICATION_PM_CLEAR = false
const val DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS = 85

data class AndroidConfiguration(
    val adbPath: File,
    val implementationModules: List<Module>,
    val autoGrantPermission: Boolean = DEFAULT_AUTO_GRANT_PERMISSION,
    val instrumentationArgs: Map<String, String> = emptyMap(),
    val applicationPmClear: Boolean = DEFAULT_APPLICATION_PM_CLEAR,
    val testApplicationPmClear: Boolean = DEFAULT_TEST_APPLICATION_PM_CLEAR,
    val installOptions: List<String> = emptyList(),
    val preferableRecorderType: DeviceFeature? = null,
    val serialStrategy: SerialStrategy = SerialStrategy.AUTOMATIC,
    val usedStorageThresholdInPercents: Int = DEFAULT_USED_STORAGE_THRESHOLD_PERCENTS
) : VendorConfiguration {

    private val koinModules = listOf(androidModule) + implementationModules

    override fun preferableRecorderType(): DeviceFeature? = preferableRecorderType

    override fun modules() = koinModules
}
