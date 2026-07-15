package com.malinskiy.marathon.vendor

import com.malinskiy.marathon.device.DeviceFeature

interface VendorConfiguration {
    fun preferableRecorderType(): DeviceFeature?

    fun createComponents(dependencies: VendorDependencies): VendorComponents
}
