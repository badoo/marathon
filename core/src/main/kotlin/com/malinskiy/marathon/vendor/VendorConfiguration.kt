package com.malinskiy.marathon.vendor

import com.malinskiy.marathon.device.DeviceFeature
import org.koin.core.module.Module

interface VendorConfiguration {
    fun preferableRecorderType(): DeviceFeature?

    fun modules(): List<Module> = emptyList()
}
