package com.malinskiy.marathon.android

import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.vendor.VendorDependencies

fun interface DeviceProviderFactory {
    fun create(dependencies: VendorDependencies, logcatListener: LogcatListener): DeviceProvider
}
